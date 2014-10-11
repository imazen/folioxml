
# What is this?

This is a full lexer, parser, and cross-compiler (to SLX, XML, and Lucene indexes) for Folio Flat File databases. 

The first conversion step is lossless, to a format called SLX. This this is like XML, but contains “ghost tags”, which come in pairs (with a matching GUID), and can start and end anywhere. This simplifies the ~120 keyword ~20 context language to ~12 keywords and normalized contexts.

The second conversion is from SLX to XML. This causes the ghost tags to be split, and is therefore nominally lossy, but lossless in reality.

From XML, we can convert to HTML, Lucene, and more. 

We even support turning query links into hyperlinks, as we have re-implemented the folio query language in the folioxml-lucene package.


## Direct conversion (no lucene index, TOC generation, or query link resolution)

This library makes it very easy to customize accurate transformations of both formatting and content. 

`DirectXhtmlExporter` and `DirectXmlExporter` are probably the clases you want to use (core/folioxml/src/folioxml/directexport).

        File f = new File(sourceFile);
        SlxRecordReader srr = new SlxRecordReader(f);
        srr.silent = false;
        String xhtmlFile = f.getParent() + File.separatorChar + f.getName()+ new SimpleDateFormat("-dd-MMM-yy-(s)").format(new Date()) + ".xhtml";
        
        DirectXhtmlExporter xh = new DirectXhtmlExporter(srr,xmlFile);
        xh.processAll();
        xh.close();
        srr.close();

## Conversion with intermediate lucene index

For conversion with query link resolution, see `contrib/folioxml-lucene/testsrc/folioxml/directexport/SimultaneousTest.java`

## Dependencies

* JDK 1.6 (For OSX, see http://support.apple.com/kb/DL1572)
* junit-4.11
* commons-cli-1.2
* commons-lang-2.4
* (folioxml-lucene only) lucene-core-3.3.0
* (folioxml-lucene only) lucene-highlighter-3.3.0


## License

Copyright 2009-2014 Imazen LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

### diff_match_patch is also Apache 2.0 licensed

 * Diff Match and Patch
 *
 * Copyright 2006 Google Inc.
 * http://code.google.com/p/google-diff-match-patch/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.