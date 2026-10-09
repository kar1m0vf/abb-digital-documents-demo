UPDATE document_orders SET order_number = CONCAT(SUBSTRING(order_number,1,9), LPAD(CAST(CAST(SUBSTRING(order_number,10,6) AS INT)+5000 AS VARCHAR),6,'0')) WHERE CAST(SUBSTRING(order_number,10,6) AS INT)<10000;
SELECT MAX(order_number) FROM document_orders;
