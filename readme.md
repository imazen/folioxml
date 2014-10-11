
# What is this?

This is a full lexer, parser, and cross-compiler for Folio Flat File databases. 

The first conversion is lossless, to a format called SLX. This this is like XML, but contains “ghost tags”, which come in pairs (with a matching GUID), and can start and end anywhere.

The second conversion is from SLX to XML. This causes the ghost tags to be split, and is therefore nominally lossy, but lossless in reality.

From XML, we can convert to HTML, ePub, Lucene, and more. 

We even support turning query links into hyperlinks, as we have re-implemented the folio query language in the folioxml-lucene package.


## Apache 2.0 License

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