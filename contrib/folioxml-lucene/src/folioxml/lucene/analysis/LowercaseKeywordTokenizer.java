package folioxml.lucene.analysis;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.lucene.analysis.CharTokenizer;
import org.apache.lucene.util.Version;

import java.io.Reader;


/**
 * Emits the entire input as a single token. Normalizes to lowercase
 */
public class LowercaseKeywordTokenizer extends CharTokenizer {
    
  public LowercaseKeywordTokenizer(Reader in) {
    super(Version.LUCENE_33, in);
  }
  @Override
  protected int normalize(int c) {
    return Character.toLowerCase(c);
  }
  
  /** Collects only characters which satisfy
   * {@link Character#isLetter(char)}.*/
  protected boolean isTokenChar(int c) {
      return true;
  }
}
