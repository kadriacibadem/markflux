# Hata yönetimi

| Durum | Ne olur? |
|---|---|
| İstemci aynı siparişi tekrar gönderir | Aynı `Idempotency-Key` ile ilk sipariş geri döner, yeni sipariş oluşmaz. |
| Cart veya Product servisi çalışmıyor | Sipariş oluşmaz, `503` döner. İstemci aynı anahtarla tekrar dener. |
| Bir servis gateway'e cevap vermiyor | Gateway `503` döner. Hatalar artarsa circuit breaker servisi bir süre hiç çağırmaz. |
| Kafka çalışmıyor | Event'ler outbox tablosunda bekler, Kafka gelince sırayla gönderilir. |
| Aynı event iki kez gelir | Inbox tablosu sayesinde ikinci kez işlenmez (bkz. aşağıdaki teslim garantisi). |
| Event işlenirken hata olur | 4 kez tekrar denenir (0,5 / 1 / 2 / 4 sn), olmazsa `<topic>.<servis>.dlt` topic'ine gönderilir. |
| Event bozuk (id/tip yok, JSON okunamıyor) | Tekrar denenmez, doğrudan DLT topic'ine gönderilir. |
| Stok yetmez | Sipariş `FAILED` olur, hiçbir stok ayrılmaz, sepet korunur. |
| Inventory cevap vermez | 2 dakika sonra sipariş `FAILED` olur ve ayrılan stok varsa geri verilir. |
| Onaylanmış sipariş iptal edilir | Stok geri eklenir. |
| Elle açılan rezervasyon 15 dk içinde onaylanmaz | Süresi dolunca en geç 1 dk içinde bırakılır, stok geri verilir. |
| Redis çalışmıyor | Ürünler veritabanından okunur, istek sınırı geçici olarak uygulanmaz. |

## Event teslim garantisi

Event'ler **en az bir kez** (at-least-once) teslim edilir. "Tam bir kez" (exactly-once) garantisi yoktur.

Outbox relay 0,5 saniyede bir çalışır. Bekleyen event'leri en fazla 100'lük gruplar halinde, kayıt sırasıyla
Kafka'ya gönderir. Her event gönderildikten sonra `published_at` alanı doldurulur, grup tek transaction'da kaydedilir.
Kafka'ya gönderim ile veritabanı kaydı tek bir transaction değildir, bu yüzden bazı durumlarda aynı event tekrar gider:

- Grup gönderilirken servis çökerse, o gruptan gönderilmiş event'ler tekrar gönderilir.
- Kafka mesajı kaydeder ama cevap ağda kaybolursa relay bunu hata sayar ve event'i tekrar gönderir.

Bu durum sorun çıkarmaz, çünkü:

- Tekrar gönderilen event'in `ce_id` değeri aynıdır (outbox kaydının id'si).
- Her consumer event'i işlemeden önce `ce_id` değerini `inbox_message` tablosuna yazar. Aynı id ikinci kez gelirse
  event atlanır.

Tekrar sayısını azaltmak için:

- Relay, Kafka producer'ın kesin cevabını bekler (en fazla 30 sn). Producer arka planda hâlâ denerken event'i
  ikinci kez göndermez.
- Producer idempotent çalışır (`acks=all`, `enable.idempotence=true`). Kendi iç tekrar denemeleri kopya üretmez.
- Bir event gönderilemezse sıradaki event'ler beklenir, böylece sıra bozulmaz.
- Aynı servisin birden fazla kopyası çalışsa bile relay'i aynı anda sadece biri çalıştırır (PostgreSQL advisory lock).

## Zaman aşımı ve tekrar deneme

- Gateway: servise bağlanma 5 sn, cevap 10 sn.
- Feign: bağlanma 1 sn, okuma 3 sn. Kendisi tekrar denemez; istemci aynı `Idempotency-Key` ile tekrar dener.
- Circuit breaker: çağrıların yarısı başarısız olursa 10 sn boyunca servisi çağırmaz.
- Outbox relay: Kafka cevabı için en fazla 30 sn bekler. Gönderilemeyen event 0,5 sn sonra tekrar denenir.
