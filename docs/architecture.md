# Mimari

## Genel yapı

- **API Gateway** tek giriş noktasıdır. İstekleri ilgili servise yönlendirir, JWT kontrolü yapar ve istek sınırı uygular.
- **Product, Inventory, Cart ve Order** servisleri kendi işlerinden ve kendi veritabanlarından sorumludur.
- **common** modülü servislerin ortak kodunu içerir (hata cevapları, güvenlik yardımcıları, outbox ve inbox).
  Servisler bu kodu `scanBasePackages = "com.markflux"` ile kullanır.

## Servisler arası iletişim

- **Senkron (Feign):** Cevaba hemen ihtiyaç olduğunda kullanılır.
  - Cart, sepete ürün eklerken Product'tan ürünü kontrol eder.
  - Order, sipariş oluştururken Cart'tan sepeti ve Product'tan güncel fiyatları alır.
- **Asenkron (Kafka):** Başka bir servisin verisini değiştiren işlemler için kullanılır.
  - Sipariş oluşunca stok ayırma işlemi Kafka üzerinden başlar.
  - Ürün değişiklikleri sepet ve stok servislerine Kafka ile duyurulur.

## Veri tutarlılığı

- Bir servis veritabanına yazdığı değişikliği ve buna ait event'i aynı transaction içinde kaydeder (outbox).
- Event'ler arka planda Kafka'ya gönderilir. Teslim garantisi "en az bir kez"dir (bkz. [failure-handling.md](failure-handling.md)).
- Aynı event iki kez gelse bile bir kez işlenir (inbox).
- Sipariş ve stok arasında dağıtık transaction yoktur; bunun yerine saga kullanılır (bkz. [event-flow.md](event-flow.md)).

## Güvenlik

- Token'lar ortak bir gizli anahtarla (HS256) imzalanır. Anahtar `JWT_SECRET` ortam değişkeninden okunur.
- Anahtar en az 32 karakter olmalıdır, kısa olursa servisler açılmaz. Varsayılan anahtar sadece geliştirme içindir.
- Gateway ve servislerin hepsi token'ı aynı anahtarla doğrular.
- Order servisi Cart'ı ve Product'ı çağırırken kullanıcının token'ını aynen iletir.
- Geliştirme ortamında (`dev` profili) token'ları gateway üretir (`POST /auth/token`).

## İzleme

- Prometheus metrikleri toplar, Grafana gösterir. Alarm kuralları `monitoring/prometheus/alerts.yml` içindedir.
- Servisler JSON log yazar, Alloy bunları Loki'ye gönderir.
- Trace'ler Tempo'ya gider. Trace bilgisi outbox kaydında saklandığı için bir siparişin gateway'den Kafka'ya,
  oradan diğer servislere kadar tüm adımları tek trace'te görülür.
