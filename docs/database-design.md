# Veritabanı tasarımı

Her servisin kendi veritabanı ve kendi veritabanı kullanıcısı vardır. Bir servis başka bir servisin veritabanına
bağlanamaz. Tablolar Flyway ile oluşturulur (`src/main/resources/db/migration`).

| Veritabanı | Tablolar |
|---|---|
| product_db | `product`, `category` |
| inventory_db | `inventory`, `stock_reservation`, `stock_reservation_line`, `stock_movement` |
| cart_db | `cart`, `cart_item` |
| order_db | `orders`, `order_item`, `order_status_history` |

Her veritabanında ayrıca aynı yapıda iki tablo vardır (`V2__create_messaging_tables.sql`):

- `outbox_event`: Kafka'ya gönderilecek event'ler. Gönderilenler 7 gün sonra silinir.
- `inbox_message`: İşlenmiş event'lerin id'leri (aynı event'in tekrar işlenmemesi için). 14 gün sonra silinir.

## Önemli kurallar

- Stok eksiye düşemez ve ayrılan stok mevcut stoktan fazla olamaz (veritabanı `CHECK` kuralı).
- Bir siparişin sadece bir stok rezervasyonu olabilir (`order_id` tekil).
- Aynı kullanıcı aynı `Idempotency-Key` ile ikinci sipariş oluşturamaz.
- Bir sepette aynı ürün bir kez bulunur, adet 1 ile 99 arasıdır.
- Siparişteki ürün adı ve fiyatı sipariş anında kopyalanır; ürün sonradan değişse de sipariş değişmez.

## Aynı anda gelen istekler

- Stok ayırırken ilgili stok satırları ürün id sırasıyla kilitlenir (`SELECT ... FOR UPDATE`). Sıra sabit olduğu
  için deadlock oluşmaz.
- Aynı kullanıcının sepetine aynı anda gelen istekler sepet satırı kilitlenerek sırayla işlenir.
- Ürün, stok, rezervasyon ve sipariş tablolarında `version` alanı ile çakışma kontrol edilir.
