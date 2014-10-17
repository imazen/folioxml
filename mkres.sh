mkdir path path/to path/to/index path/to/index/lucene-index/ path/to/index/export/ path/to/fff/
mkdir files files/indexes files/folio-help files/folio-help/export/
mkdir tmp
cd tmp
unzip ../FolioHlp.zip
cp FolioHlp.FFF ../files/folio-help
cp FolioHlp.DEF ../files/folio-help
cd ..
rm -rf tmp
