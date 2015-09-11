master: [![status](http://img.shields.io/travis/imazen/folioxml/master.svg)](https://travis-ci.org/imazen/folioxml/branches)
 [![Build status](https://ci.appveyor.com/api/projects/status/dor7s9akmby228cb/branch/master?svg=true)](https://ci.appveyor.com/project/imazen/folioxml/branch/master)

develop: [![status](http://img.shields.io/travis/imazen/folioxml/develop.svg)](https://travis-ci.org/imazen/folioxml/branches)
[![Build status](https://ci.appveyor.com/api/projects/status/dor7s9akmby228cb/branch/develop?svg=true)](https://ci.appveyor.com/project/imazen/folioxml/branch/develop)

# What is this?

This is a full streaming lexer, parser, and transpiler for Folio Flat File databases. Outputs include SLX, XML, HTML, and Lucene. Stream-based (not DOM-based) - can process gigabyes quickly with very low RAM use.

The first conversion step is lossless, to a format called SLX. This this is like XML, but contains “ghost tags”, which come in pairs (with a matching GUID), and can start and end anywhere. This simplifies the ~120 keyword ~20 context language to ~12 keywords and 2 contexts.

The second conversion is from SLX to XML. This causes the ghost tags to be split, and is therefore nominally lossy, but lossless in reality.

From XML, we can convert to HTML, Lucene, and more. 

We even support turning query links into hyperlinks, as we have re-implemented the folio query language in the folioxml-lucene package.

Our XML implementation offers regex-based search and replace that only affects text contents of nodes - and uses diff_match_patch underneath to minimize shift between container elements. VirutalCharSequence provides the abstraction for modifying the text of an XML tree as a single string. 

Contact Imazen for custom development, conversion, and support services. support@imazen.io

## Considerations

* Copy and rename referenced images, OLE objects, and embedded data links/PDFs.
* Convert bitmaps to PNG.
* Index infoabases using folio indexing rules so that query links can be evaluated.
* Index multiple infobases to a single lucene index so cross-infobase queries and links can be resolved
* Split at any record level; offer an interface to determine splitting rules.

## Does not support

* User links
* Query templates
* Hypergraphics (image maps) or OLE objects or metafiles
* Named popups (yet)


## Dependencies

* JDK 1.8
* junit-4.12
* commons-cli-1.3.1
* commons-lang-3.4
* (folioxml-lucene only) lucene 5.3.0

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

Within a yaml file, each root key represents an infobase set; a configuration file can have many different configurations within.
It is important that both the IDs and infobase set keys are valid XML identifiers, as they will be used in CSS and XML/HTML. Here we have a yaml file with just one named configuration 'myconfig'

    myconfig:

      export_xml: true   #Creates a single XML file with all the content (excluding the stylesheet, images, and logs)  (default=true)
      skip_normal_records: false # affects xml only - only writes out hierarchy-affecting levels, in order to make a shorter file. (default=false)
      nest_file_elements: true # affects XML only - Disables nested syntax for <file> elements in xml, uses flat structure. Nesting uses same hierarchy as folio. (default=true)
      indent_xml: true #Indentation can introduce undesired visual artificacts/spacing, and should only be used for human consumption. (default=true)


      export_hidden_text: true # Halves performance. Writes out a log of text that would be hidden by the generated CSS (either via display:none or zero-contrast coloring). (default=true)

      resolve_jump_links: true # Disable fixing up jump links (default=true)
      resolve_query_links: true # Disable simulating queries and linking them to the first result. If this and resolve_jump_links are false, no Lucene index is required. (default=true)

      export_html: false # Exports lots of browsable HTML files according to the splitting and naming rules. (default=true)
      use_highslide: false #affects both XML and HTML. Required for popups/notes to keep working. (default=true)
      add_nav_links: true #html only # Adds prev/next links at the beginning and end of each HTML generated. (default=true)

      faux_tabs: false # Enable faux tabs (default=false)
      faux_tabs_window_min: 80 # We have to deal with centered and right-aligned tabs.
      faux_tabs_window_max: 120 # These provide the default and maximum (character count) bounds with which to simulate them.

      # Here we can manually map broken URLs (and cross-infobase links) to new places
      link_mapper:
          urls:
            'C:\Files\Data.pdf': 'https://othersite/data'

          infobases:
            'C:\Files\Other.NFO': 'http://othersite/other'
            'C:\Files\Other2.NFO': 'http://othersite/other2'

      #This is how we trash stuff we don't care about
      pull: # log_pulled_elements.txt and log_dropped_elements.txt are created
        program_links: true
        menu_links: true
        drop_notes: false # It can be cleaner to drop notes/popups than preserve them with highslide & javascript. Dropped data is logged.
        drop_popups: false # When use_highslide is false, the popups would otherwise be invalid HTML
        ole_objects: false
        metafile_objects: false
        links_to_infobases:
        - 'C:\Files\Obsolete.NFO'
        - 'C:\Files\Obsolete2.NFO'

      #You must convert all your infobases that link to each other at once, otherwise those links will not be preserved.
      #In addition, unique IDs will overlap between infobases converted separately, causing potential issues in your final data store.
      infobases:
        - id: info_a
          path: "files/info_a.FFF"
          aliases:
            - 'C:\files\info_a.NFO'
        - id: info_b
          path: "files/info_b.FFF"
          aliases:
            - 'C:\files\info_b.NFO


      #Structure affects how we split the infobase into parts and identify those parts.
      # We can specify a custom provider class

      structure_class: "folioxml.export.structure.IdSlugProvider"
      structure_class_params: [ "null",  "null", 0, 1, 1]

      #structure_class_params: String levelRegex, String splitOnFieldName, Integer idKind, Integer root_index, Integer start_index

      # levelRegex lets us split based on predefined folio levels like "Heading 1|Heading 2"
      # splitOnFieldName lets us split whenever a record contains the given field.
      # idKind values
      # 0 (heading-based slugs), 1 (integers), 2 (nested integers 2.3.1), 3 (guids), or 4 (folio IDs). Schemes 5-9 use the contents of splitOnFieldName and fall back to 0-4 if missing or non-unique.
      # root_index and start_index are used for idKinds 2 and 3 (as well as 7 and 8, of course).

      asset_start_index: 1 #What index do we start with for asset IDs.

      #Only set this to true if you also set export_locations: images: url to a template that can respond based on numeric ID instead of filename. See AssetInventory.xml
      asset_use_index_in_url: true

      export_locations:
        default:
          path: "files/export/{id}-{stamp}/{input}" #Used for text, xml files, css, and logs
        image:
          path: files/export/{input}  #Adjust this to represent the absolute path where the static images will be stored. Control {input} with asset_use_index_in_url
        luceneindex:
          path: "files/indexes/myconfig/"    #Just a temp directory.
        html:
          path: "files/export/{id}-{stamp}/html/{input}.html" #Adjust this to represent the final hosted location of your HTML pages, as an absolute path. Adjust idKind to change {input}


The above configuration, if named conf.yaml, could be invoked like this:

* mvn clean compile assembly:single -U -B -fae
* java -jar commandline/target/folioxml-commandline-jar-with-dependencies.jar -config core/folioxml/resources/conf.yaml -export myconfig


## Running the command line app

* mvn clean compile assembly:single -U -B -fae
* java -jar commandline/target/folioxml-commandline-jar-with-dependencies.jar -config core/folioxml/resources/test.yaml -export folio_help

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
