master: [![status](http://img.shields.io/travis/imazen/folioxml/master.svg)](https://travis-ci.org/imazen/folioxml/branches) most recent commit: [![status](http://img.shields.io/travis/imazen/folioxml.svg)](https://travis-ci.org/imazen/folioxml/branches) 


# What is this?

This is a full streaming lexer, parser, and transpiler for Folio Flat File databases. Outputs include SLX, XML, HTML, and Lucene. Stream-based (not DOM-based) - can process gigabyes quickly with very low RAM use.

The first conversion step is lossless, to a format called SLX. This this is like XML, but contains “ghost tags”, which come in pairs (with a matching GUID), and can start and end anywhere. This simplifies the ~120 keyword ~20 context language to ~12 keywords and 2 contexts.

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

You can copy/paste or subclass DirectHtmlExporter to customize which node list processors you want to apply (core/folioxml/src/folioxml/export/html contains many options).

DirectXhtmlExporter uses these by default

        NodeList nodes =  MultiRunner.process(new NodeList(rx),
                new RecordAnchorWriter(),
                new BookmarksAndJumpLinks(), //Drop x-infobase links, fix jump links and destinations
                new Images(), //Convert eligible object tags into img tags
                new Notes(),  //Adds js notes
                new Popups(), //Adds js popups
                new FixImagePaths(""), //Switch to forward slashes
                new SplitSelfClosingTags(),
                new CleanupSlxStuff()); //Removes pagebreak|ss|pp, program links, span.recordHeading, record.groups, and renames group to div.
        

You might, for example, want to add EllipsisAndDashes, which converts ... and -- to the equivalent typographical characters.

Our XML implementation offers regex-based search and replace that only affects text contents of nodes - and uses diff_match_patch underneath to minimize shift between container elements. VirutalCharSequence provides the abstraction for modifying the text of an XML tree as a single string. 


## Considerations

* Copy and rename referenced images, OLE objects, and embedded data links/PDFs.
* Convert bitmaps to PNG.
* Index infoabases using folio indexing rules so that query links can be evaluated.
* Index multiple infobases to a single lucene index so cross-infobase queries and links can be resolved
* Split at any record level; offer an interface to determine splitting rules.

## Does not support

* User links
* Query templates
* Hypergraphics (image maps)


## Conversion with intermediate lucene index

For conversion with query link resolution, see `contrib/folioxml-lucene/testsrc/folioxml/directexport/SimultaneousTest.java`

## Dependencies

* JDK 1.7
* junit-4.11
* commons-cli-1.2
* commons-lang-2.4
* (folioxml-lucene only) lucene-core-3.3.0
* (folioxml-lucene only) lucene-highlighter-3.3.0

## Test infobase

Folio Views includes an excellent test infobase, FolioHlp, which uses all documented infobase features. We cannot legally redistribute this due to copyright, but if you have Folio Views, you should have access to this. 

## Exporting .nfo to .FFF

When exporting to flat file, you should use the following options:

* Check "Write comments to flat file"
* Check "Write record IDs in record code <RD...>"
* Check "Insert a definition include code <DI...>
* Uncheck "Include full path to files referenced"
* Set Default units: Inches.

Folio Views and Folio Builder usually provide export functionality, although if the infobase is ‘restricted’, you may need to get permission from the publisher.

## Yaml configuration reference

Within a yaml file, each root key represents an infobase set; a configuration file can have many different configurations. It is important that both the IDs and infobase set keys are valid XML identifiers, as they will be used in CSS and XML/HTML.

We will describe the contents of one infobase set.

* skip_normal_records: true/false  #Exporting only headers can make it easier to analyze the structure
* indent_xml true/false #Indentation can introduce undesired spacing, and should only be used for human consumption



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
