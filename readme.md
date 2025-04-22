[![GitHub Actions Status](https://github.com/imazen/folioxml/actions/workflows/docker-publish.yml/badge.svg)](https://github.com/imazen/folioxml/actions/workflows/docker-publish.yml)
[![Docker Hub](https://img.shields.io/docker/pulls/imazen/folioxml.svg)](https://hub.docker.com/r/imazen/folioxml/)
[![Open in GitHub Codespaces](https://github.com/codespaces/badge.svg)](https://codespaces.new/imazen/folioxml?ref=main)

# FolioXML: Folio Flat File (.FFF) Conversion Tool

This is a full streaming lexer, parser, and transpiler for Folio Flat File databases (`.FFF`, exported from `.NFO`). Outputs include structured XML (SLX), standard XML, static HTML, and Lucene indexes.

## Getting Started: Converting Your FFF Files

The easiest way to use FolioXML is with Docker, either locally or in a cloud environment like GitHub Codespaces.

**Prerequisites:**

*   **Docker:** Use Codespaces, or Install [Docker Desktop](https://www.docker.com/products/docker-desktop/) or Docker Engine. 
*   **Your `.FFF` file:** You need to have exported your Folio infobase (`.NFO`) to the Folio Flat File (`.FFF`) format. See [Exporting .nfo to .FFF](#exporting-nfo-to-fff) below.
*   **(Optional) Your `objects` folder:** If your infobase contains embedded images (BMP, WMF, etc.) or other objects (OLE), ensure you also have the associated `objects` folder that was likely created during the `.FFF` export. Only certain objects can be converted automatically.

**Steps:**

1.  **Open in Codespace (Recommended) or Clone Locally:**
    *   **Codespace:** Click the "Open in GitHub Codespaces" badge above. This launches a pre-configured cloud environment with Docker and the necessary tools ready.
    *   **Local:** Clone the repository: `git clone https://github.com/imazen/folioxml.git && cd folioxml`

2.  **Copy the Example Project:**
    The `examples/folio-help` directory contains a starting template. Copy it to create your project folder:
    ```bash
    # In your Codespace or local terminal:
    cp -r examples/folio-help my_project
    cd my_project
    ```

3.  **Add Your Data:**
    *   Place your `.FFF` and `.DEF` files (and the folder of objects) inside the `my_project/input/` directory (e.g., `my_project/input/MyInfobase.FFF`, `my_project/input/MyInfobase.DEF`, `my_project/input/MyInfobase/`).


4.  **Configure the Export (`config.yaml`):**
    Edit the `my_project/config.yaml` file:
    *   Update `infobases.path`: Change `/data/input/FolioHlp.FFF` to `/data/input/MyInfobase.FFF`.
    *   Update `infobases.id`: Change `foliohelp` to a unique ID for your project (e.g., `my_infobase`). This ID is used in output filenames.
    *   *(Optional)* Add `aliases` if you plan to process multiple interlinked infobases later.
    *   Review and adjust `export_locations` if you want output in different subdirectories (relative to `/data`, which maps to your `my_project` folder).
    *   Review other options like `structure_class`, `export_html`, `resolve_query_links` etc. See the comments within `config.yaml` for detailed explanations of all options.

5.  **Run the Export:**
    Execute the appropriate script from within your `my_project` directory:
    *   **Linux/macOS/WSL/Codespaces:**
        ```bash
        chmod +x export.sh # If needed
        ./export.sh
        ```
    *   **Windows (PowerShell):**
        ```powershell
        ./export.ps1
        ```
    This script uses the `config.yaml` and runs the `imazen/folioxml` Docker container (pulling it if you don't have it locally). It mounts your `my_project` directory as `/data` inside the container. Your converted files will appear in the `export/` directory under a unique subfolder, so you can run the script multiple times with different settings.

**Handling Embedded Objects (Images, OLE):**

*   FolioXML **does not** automatically extract, convert, or update links to embedded objects (like images in BMP/WMF/HGX format or OLE objects) stored in the `objects` folder **in the default configuration/pipeline**.
*   While there is code (`RenameImages.java`) intended to help with identifying and renaming these assets (and converting BMPs), it may not cover all object path formats or be fully integrated into the standard export process invoked by the command-line tool.
*   **Therefore, the current recommended workflow requires manual handling after running the initial conversion:**
    1.  **Copy Objects Folder:** Ensure the `objects` folder from your Folio export is present in your project's `input` directory (e.g., `my_project/input/objects`).
    2.  **Copy to Output:** After running the export script (`./export.sh` or `./export.ps1`), manually copy the contents of `my_project/input/objects` to your desired final web asset location (this might be a subdirectory within `my_project/export/` like `my_project/export/images/`, or a separate assets directory).
    3.  **Convert Images:** Use external tools (like ImageMagick, Inkscape, graphics software, or custom scripts) to convert proprietary or non-web-friendly image formats (like `.bmp`, `.wmf`, `.hgl`, `.hgx`) into standard web formats (`.png`, `.jpg`, `.svg`). Place the converted images in the chosen output asset location.
    4.  **Update Links:** Search through your generated output files (HTML/XML in `my_project/export/`) for `<img>` tags or other references to the original object filenames (e.g., `<img src="objects/IMAGE001.bmp" ...>`) and update the `src` (or `href`) attributes to point to the new relative path and filename of your converted web images (e.g., `<img src="images/IMAGE001.png" ...>`). This step often requires scripting or batch find-and-replace operations tailored to your specific output structure and asset naming conventions.
*   *Future enhancements to FolioXML might provide more automated asset handling.* 

## Alternative: Building the Docker Image Manually

If you need to modify the FolioXML code or use a specific version, you can build the Docker image yourself instead of using the pre-built `imazen/folioxml` image.

1.  Clone the repository: `git clone https://github.com/imazen/folioxml.git && cd folioxml`
2.  Build the image:
    ```bash
    docker build -t my-folioxml-build .
    ```
3.  Modify the `export.sh` or `export.ps1` script in your copied example directory (`my_project`) to use your custom image name (`my-folioxml-build` instead of `folioxml-test` or `imazen/folioxml`).
4.  Follow steps 2-5 from the "Getting Started" section above, using your modified script.

## Exporting .nfo to .FFF

To use this tool, you first need to export your original Folio Views/Builder infobase (`.nfo`) to the Folio Flat File format (`.FFF`).

When exporting from Folio Views/Builder, use these settings for best results:

*   **Check:** "Write comments to flat file"
*   **Check:** "Write record IDs in record code <RD...>"
*   **Check:** "Insert a definition include code <DI...>"
*   **Uncheck:** "Include full path to files referenced" (This makes object paths relative, usually to an `objects` folder)
*   **Set:** Default units: `Inches`.

*Note: If the infobase is 'restricted', export functionality might be disabled by the publisher.* 

## Technical Details

The first conversion step is lossless, to a format called SLX. This this is like XML, but contains "ghost tags", which come in pairs (with a matching GUID), and can start and end anywhere. This simplifies the ~120 keyword ~20 context language to ~12 keywords and 2 contexts.

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

* JDK 1.8 and Maven
* junit-4.12
* commons-cli-1.3.1
* commons-lang-3.4
* (folioxml-lucene only) lucene 5.3.0

## Test infobase

Folio Views includes an excellent test infobase, FolioHlp, which uses all documented infobase features. We cannot legally redistribute this due to copyright, but if you have Folio Views, you should have access to this. 

## Yaml configuration reference

Within a yaml file, each root key represents an infobase set; a configuration file can have many different configurations within.
It is important that both the IDs and infobase set keys are valid XML identifiers, as they will be used in CSS and XML/HTML. Here we have a yaml file with just one named configuration 'myconfig'

    myconfig:

      export_xml: true   #Creates a single XML file with all the content (excluding the stylesheet, images, and logs)  (default=true)
      skip_normal_records: false # affects xml only - only writes out hierarchy-affecting levels, in order to make a shorter file. (default=false)
      nest_file_elements: true # affects XML only - Disables nested syntax for <file> elements in xml, uses flat structure. Nesting uses same hierarchy as folio. (default=true)
      indent_xml: true #Indentation can introduce undesired visual artificacts/spacing, and should only be used for human consumption. (default=true)

      export_inventory: true # Lowers performance - tracks unique elements in memory, generates a textual report at the end. (default=true)

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
          path: "files/export/{set-id}-{stamp}/{input}" #Used for text, xml files, css, and logs
        image:
          path: files/export/{input}  #Adjust this to represent the absolute path where the static images will be stored. Control {input} with asset_use_index_in_url
        luceneindex:
          path: "files/indexes/myconfig/"    #Just a temp directory.
        html:
          path: "files/export/{set-id}-{stamp}/html/{input}.html" #Adjust this to represent the final hosted location of your HTML pages, as an absolute path. Adjust idKind to change {input}


The above configuration, if named conf.yaml, could be invoked like this:

* mvn clean compile assembly:single -U -B -fae
* java -jar commandline/target/folioxml-commandline-jar-with-dependencies.jar -config core/folioxml/resources/conf.yaml -export myconfig


## Running the command line app

* mvn clean compile assembly:single -U -B -fae
* java -jar commandline/target/folioxml-commandline-jar-with-dependencies.jar -config core/folioxml/resources/test.yaml -export folio_help


## Cleanup

Search resulting text for "data-linkname" to locate data links.
Browse log_broken_query_links.txt or log_unresolved_query_links.txt if query link resolution is disabled.
Browse log_broken_jump_links.txt or log_unresolved_jump_links.txt if jump link resolution is disabled.
Browse log.txt for error messages. Errors in dealing with assets can be due to corrupted images.
Browse log_report.txt to see uniques and invalid URLs.
Browse log_tidy_invalid_elements.txt to see what elements didn't look like HTML when they came out.
Read log_pulled_elements.txt and log_dropped_elements.txt to see what you had removed.
Read log_mapped_links.txt to see how your URL mapping rules were applied.

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


For testing, https://public-unit-test-resources.s3.us-east-1.amazonaws.com/FolioHlp.zip  contains a .FFF, .DEF, and folder of objects as exported from a nfo.


![YourKit](https://www.yourkit.com/images/yklogo.png)

YourKit supports open source projects with its full-featured Java Profiler.
YourKit, LLC is the creator of <a href="https://www.yourkit.com/java/profiler/index.jsp">YourKit Java Profiler</a>
and <a href="https://www.yourkit.com/.net/profiler/index.jsp">YourKit .NET Profiler</a>, innovative and intelligent tools for profiling Java and .NET applications.
