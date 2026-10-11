# Servislerin görevleri

## API Gateway
- İstekleri doğru servise yönlendirir.
- JWT doğrular ve basit yetki kurallarını uygular.
- Kullanıcı veya IP başına istek sınırı uygular (Redis, saniyede 10 istek, en fazla 40'lık ani artış).
- Çalışmayan bir servis için hızlıca `503` döner (circuit breaker).
- Her cevaba `X-Trace-Id` ekler.
- `dev` profilinde test için token üretir.

## Product Service
- Ürün ve kategori oluşturur, ürünü günceller, aktif/pasif yapar.
- Ürünleri isim, kategori, fiyat ve id listesine göre arar. Admin olmayanlar sadece aktif ürünleri görür.
- Ürün detayını Redis'te 10 dakika cache'ler.
- Ürün değişikliklerini Kafka'ya gönderir.

## Inventory Service
- Yeni ürün oluşunca (`ProductCreated`) ürün için boş bir stok kaydı açar.
- Ürün başına stok miktarını ve ayrılmış miktarı tutar.
- Sipariş için stok ayırır; stok yetmezse siparişin hiçbir satırını ayırmaz.
- Sipariş onaylanınca stoğu düşer, iptal olunca veya başarısız olunca geri verir.
- Elle açılan rezervasyonları süresi (15 dk) dolunca bırakır.
- Her stok değişikliğini hareket olarak kaydeder.

## Cart Service
- Her kullanıcının bir sepeti vardır.
- Sepete ürün ekler, adedini değiştirir, siler. Bir sepette en fazla 50 ürün, ürün başına en fazla 99 adet olur.
- Ürün değişince (`ProductUpdated`, `ProductStatusChanged`) sepetteki ad, fiyat ve satışta olma bilgisini günceller.
- Sepetteki fiyat sadece gösterim içindir; gerçek fiyat siparişte alınır.
- Sipariş onaylanınca sipariş edilen ürünleri sepetten çıkarır.

## Order Service
- Sepetten sipariş oluşturur. `Idempotency-Key` ile aynı sipariş iki kez oluşmaz.
- Fiyatları Product servisinden alır, istemcinin gönderdiği tutara güvenmez.
- Sipariş durumunu yönetir: `PENDING → CONFIRMED → SHIPPED → DELIVERED`, ayrıca `CANCELLED` ve `FAILED`.
- 2 dakika içinde stok ayrılmayan siparişi `FAILED` yapar.
