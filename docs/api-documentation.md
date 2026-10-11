# API dokümantasyonu

Swagger UI: http://localhost:8080/swagger-ui.html

Postman: [MarkFlux.postman_collection.json](../postman/MarkFlux.postman_collection.json) dosyasını Postman'e import et.
Önce **Auth** klasöründen token'ları al; token'lar ve oluşturulan kayıtların id'leri collection değişkenlerine
otomatik yazılır.

Tüm istekler gateway (`localhost:8080`) üzerinden yapılır. Yetki gereken isteklerde
`Authorization: Bearer <token>` başlığı gönderilir.

## Hata cevabı

Hatalar aynı formatta (`application/problem+json`) döner:

```json
{
  "title": "Not Found",
  "status": 404,
  "detail": "Product not found: ...",
  "instance": "/api/products/...",
  "errorCode": "PRODUCT_NOT_FOUND",
  "timestamp": "2026-10-11T10:15:30Z",
  "traceId": "6f1c2d0e9b7a4c3d8e2f1a0b9c8d7e6f"
}
```

## Endpoint'ler

| Metod | Yol | Yetki | Açıklama |
|---|---|---|---|
| POST | `/auth/token` | herkes | Geliştirme token'ı al (sadece `dev` profili) |
| POST | `/api/products` | admin | Ürün oluştur |
| GET | `/api/products` | herkes | Ürün ara (`q`, `categoryId`, `minPrice`, `maxPrice`, `ids`, `status`, `page`, `size`, `sort`) |
| GET | `/api/products/{id}` | herkes | Ürün detayı |
| PUT | `/api/products/{id}` | admin | Ürün güncelle |
| PATCH | `/api/products/{id}/status` | admin | Ürünü aktif/pasif yap |
| POST | `/api/categories` | admin | Kategori oluştur |
| GET | `/api/categories` | herkes | Kategorileri listele |
| GET | `/api/inventory/{productId}` | herkes | Stok bilgisi |
| PUT | `/api/inventory/{productId}` | admin | Stok miktarını ayarla |
| POST | `/api/inventory/{productId}/adjustments` | admin | Stok ekle/çıkar (`quantityChange`) |
| GET | `/api/inventory/{productId}/movements` | admin | Stok hareketleri |
| POST | `/api/inventory/reservations` | admin | Elle stok rezervasyonu yap |
| GET | `/api/inventory/reservations/{id}` | admin | Rezervasyon detayı |
| POST | `/api/inventory/reservations/{id}/confirm` | admin | Rezervasyonu onayla |
| POST | `/api/inventory/reservations/{id}/release` | admin | Rezervasyonu bırak |
| GET | `/api/cart` | kullanıcı | Sepetim |
| POST | `/api/cart/items` | kullanıcı | Sepete ürün ekle |
| PATCH | `/api/cart/items/{productId}` | kullanıcı | Adet değiştir |
| DELETE | `/api/cart/items/{productId}` | kullanıcı | Ürünü çıkar |
| DELETE | `/api/cart` | kullanıcı | Sepeti boşalt |
| POST | `/api/orders` | kullanıcı | Sipariş ver (`Idempotency-Key` başlığı zorunlu) |
| GET | `/api/orders` | kullanıcı | Siparişlerim |
| GET | `/api/orders/{id}` | kullanıcı | Sipariş detayı |
| POST | `/api/orders/{id}/cancel` | kullanıcı | Siparişi iptal et |
| PATCH | `/api/orders/{id}/status` | admin | Kargo durumu (`SHIPPED`, `DELIVERED`) |

## Notlar

- `GET /api/products` admin olmayanlara sadece aktif ürünleri döner.
- `POST /api/orders` yeni siparişte `202`, aynı `Idempotency-Key` tekrar gönderilince `200` döner.
  Tekrar olup olmadığı `Idempotent-Replayed` başlığında yazar.
- Kullanıcı sadece kendi siparişini görür ve iptal eder. Admin tüm siparişleri görebilir.
- Rezervasyon endpoint'leri elle müdahale içindir; sipariş akışı rezervasyonu Kafka üzerinden kendisi yönetir.
  Elle açılan rezervasyon 15 dakika içinde onaylanmazsa otomatik olarak bırakılır.
