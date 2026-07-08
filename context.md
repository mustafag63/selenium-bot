# Context Notes

- MAX_CONCURRENT=18 + exponential(8.5s) varış modeli resmi olarak doğrulandı
  (L=11.94≈12 hedef, FAIL oranı %1.8, browsing/formfilling KS testleri temiz).
  Searching persona'sında KS-stat≈0.15 seviyesinde tutarlı (iki ayrı 15-30dk
  koşuda tekrarlanan) ama sınırlı bir timing sapması gözlemlendi. addToCart/
  alert-wait etkileşimi hipotezi test edilip çürütüldü (CART vs NOCART grupları
  arasında medyan farkı yok). Kök neden muhtemelen yüksek concurrency altında
  sayfa-yükleme/CDP gecikmesi, izole edilmedi — kabul edilebilir seviyede
  kalındığı için (proje ölçütü: birebir CICIDS uyumu değil, gerçekçi referans)
  daha fazla araştırılmadı, dataset üretimine geçildi.
