/*
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
package org.apache.stanbol.contenthub.search.featured;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.SolrParams;
import org.apache.stanbol.contenthub.index.ldpath.LDPathSemanticIndex;
import org.apache.stanbol.contenthub.search.solr.util.SolrQueryUtil;
import org.apache.stanbol.contenthub.servicesapi.Constants;
import org.apache.stanbol.contenthub.servicesapi.index.IndexException;
import org.apache.stanbol.contenthub.servicesapi.index.IndexManagementException;
import org.apache.stanbol.contenthub.servicesapi.index.SemanticIndexManager;
import org.apache.stanbol.contenthub.servicesapi.index.search.SearchException;
import org.apache.stanbol.contenthub.servicesapi.index.search.featured.FacetResult;
import org.apache.stanbol.contenthub.servicesapi.index.search.featured.FeaturedSearch;
import org.apache.stanbol.contenthub.servicesapi.index.search.featured.SearchResult;
import org.apache.stanbol.contenthub.servicesapi.index.search.related.RelatedKeyword;
import org.apache.stanbol.contenthub.servicesapi.index.search.related.RelatedKeywordSearchManager;
import org.apache.stanbol.contenthub.servicesapi.index.search.solr.SolrSearch;
import org.apache.stanbol.contenthub.servicesapi.store.vocabulary.SolrVocabulary.SolrFieldName;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EnhancementException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.impl.ByteArraySource;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
@Service
public class FeaturedSearchImpl implements FeaturedSearch {

    private final static Logger log = LoggerFactory.getLogger(FeaturedSearchImpl.class);

    private static Map<String,List<String>> stopWords;

    static {
        stopWords = new HashMap<String,List<String>>();
        // TODO read stopwords from the files located in default solr core zip
        List<String> englishStopWords = Arrays.asList("i", "me", "my", "myself", "we", "our", "ours",
            "ourselves", "you", "your", "yours", "yourself", "yourselves", "he", "him", "his", "himself",
            "she", "her", "hers", "herself", "it", "its", "itself", "they", "them", "their", "theirs",
            "themselves", "what", "which", "who", "whom", "this", "that", "these", "those", "am", "is",
            "are", "was", "were", "be  ", "been", "being", "have", "has ", "had ", "having", "do", "does",
            "did ", "doing", "would", "should", "could", "ought", "a", "an", "the", "and", "but", "if", "or",
            "because", "as", "until", "while", "", "of", "at", "by", "for", "with", "about", "against",
            "between", "into", "through", "during", "before", "after", "above", "below", "to", "from", "up",
            "down", "in", "out", "on", "off", "over", "under", "again", "further", "then", "once", "here",
            "there", "when", "where", "why", "how", "all", "any", "both", "each", "few", "more", "most",
            "other", "some", "such", "no", "nor", "not", "only", "own", "same", "so", "than", "too", "very",
            "i'm", "you're", "he's", "she's", "it's", "we're", "they're", "i've", "you've", "we've",
            "they've", "i'd", "you'd", "he'd", "she'd", "we'd", "they'd", "i'll", "you'll", "he'll",
            "she'll", "we'll", "they'll", "isn't", "aren't", "wasn't", "weren't", "hasn't", "haven't",
            "hadn't", "doesn't", "don't", "didn't", "won't", "wouldn't", "shan't", "shouldn't", "can't",
            "cannot", "couldn't", "mustn't", "let's", "that's", "who's", "what's", "here's", "there's",
            "when's", "where's", "why's", "how's", "of");
        stopWords.put("en", englishStopWords);
    }

    @Reference
    private SolrSearch solrSearch;

    @Reference
    private SemanticIndexManager semanticIndexManager;

    @Reference
    private RelatedKeywordSearchManager relatedKeywordSearchManager;

    @Reference
    private EnhancementJobManager enhancementJobManager;

    @Reference
    private ContentItemFactory contentItemFactory;

    // private BundleContext bundleContext;
    //
    // @Activate
    // public void activate(ComponentContext context) {
    // this.bundleContext = context.getBundleContext();
    // }

    private List<FacetResult> convertFacetFields(List<FacetField> facetFields, List<FacetResult> allFacets) {
        List<FacetResult> facets = new ArrayList<FacetResult>();
        if (allFacets == null) {
            for (FacetField facetField : facetFields) {
                if (facetField.getValues() != null) {
                    facets.add(new FacetResultImpl(facetField));
                }
            }
        } else {
            for (FacetField facetField : facetFields) {
                if (facetField.getValues() != null) {
                    for (FacetResult facetResult : allFacets) {
                        if (facetResult.getFacetField().getName().equals(facetField.getName())) {
                            facets.add(new FacetResultImpl(facetField, facetResult.getType()));
                        }
                    }
                }
            }
        }
        return facets;
    }

    @Override
    public SearchResult search(String queryTerm, String ontologyURI, String indexName) throws SearchException {
        QueryResponse queryResponse = solrSearch.search(queryTerm, indexName);
        return search(queryTerm, queryResponse, ontologyURI, indexName, null);
    }

    private SearchResult search(String queryTerm,
                                QueryResponse queryResponse,
                                String ontologyURI,
                                String indexName,
                                List<FacetResult> allFacets) throws SearchException {
        List<String> resultantDocuments = new ArrayList<String>();
        for (SolrDocument solrDocument : queryResponse.getResults()) {
            Object uri = solrDocument.getFieldValue(SolrFieldName.ID.toString());
            if (uri != null) {
                resultantDocuments.add(uri.toString());
            }
        }
        Map<String,Map<String,List<RelatedKeyword>>> relatedKeywords = new HashMap<String,Map<String,List<RelatedKeyword>>>();
        List<String> queryTerms = tokenizeEntities(queryTerm);

        for (String queryToken : queryTerms) {
            relatedKeywords.putAll(relatedKeywordSearchManager.getRelatedKeywordsFromAllSources(queryToken,
                ontologyURI).getRelatedKeywords());
        }
        return new FeaturedSearchResult(resultantDocuments, convertFacetFields(
            queryResponse.getFacetFields(), allFacets), relatedKeywords);
    }

    @Override
    public SearchResult search(SolrParams solrParams, String ontologyURI, String indexName) throws SearchException {
        /*
         * RESTful services uses search method with "SolrParams" argument. For those operations
         */
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.add(solrParams);
        List<FacetResult> allFacets = getAllFacetResults(indexName);
        SolrQueryUtil.setDefaultQueryParameters(solrQuery, allFacets);
        QueryResponse queryResponse = solrSearch.search(solrQuery, indexName);
        String queryTerm = SolrQueryUtil.extractQueryTermFromSolrQuery(solrParams);
        return search(queryTerm, queryResponse, ontologyURI, indexName, allFacets);
    }

    @Override
    public List<FacetResult> getAllFacetResults(String indexName) throws SearchException {
        LDPathSemanticIndex semanticIndex = null;
        try {
            semanticIndex = (LDPathSemanticIndex) semanticIndexManager.getIndex(indexName);
        } catch (IndexManagementException e) {
            log.error("Failed to get index {}", indexName, e);
            throw new SearchException("Failed to get index " + indexName, e);
        }
        List<FacetResult> facetResults = new ArrayList<FacetResult>();
        List<String> fieldsNames = new ArrayList<String>();
        try {
            fieldsNames = semanticIndex.getFieldsNames();
            for (int i = 0; i < fieldsNames.size(); i++) {
                String fn = fieldsNames.get(i);
                String type = (String) semanticIndex.getFieldProperties(fn).get("type");
                facetResults.add(new FacetResultImpl(new FacetField(fn), type.trim()));
            }
        } catch (IndexException e) {
            log.error(e.getMessage(), e);
            throw new SearchException(e.getMessage(), e);
        }

        return facetResults;
    }

    @Override
    public List<String> tokenizeEntities(String queryTerm) {
        // obtain entities about query term through Enhancer
        ContentItem ci = null;
        boolean error = false;
        try {
            ci = contentItemFactory.createContentItem(new ByteArraySource(queryTerm
                    .getBytes(Constants.DEFAULT_ENCODING), "text/plain"));
            enhancementJobManager.enhanceContent(ci);
        } catch (UnsupportedEncodingException e) {
            log.error("Failed to get bytes of query term: {}", queryTerm, e);
            error = true;
        } catch (EnhancementException e) {
            log.error("Failed to get enmancements for the query term: {}", queryTerm, e);
            error = true;
        } catch (IOException e) {
            log.error("Failed to get bytes of query term: {}", queryTerm, e);
            error = true;
        }

        List<String> tokenizedTerms = new ArrayList<String>();
        if (error || ci == null || ci.getMetadata() == null) {
            tokenizedTerms.add(queryTerm);
        } else {
            // traverse selected text assertions
            MGraph queryTermMetadata = ci.getMetadata();
            Iterator<Triple> textAnnotations = queryTermMetadata.filter(null,
                Properties.ENHANCER_SELECTED_TEXT, null);
            while (textAnnotations.hasNext()) {
                Resource r = textAnnotations.next().getObject();
                String selectedText = "";
                if (r instanceof Literal) {
                    selectedText = ((Literal) r).getLexicalForm();
                } else {
                    selectedText = r.toString();
                }

                tokenizedTerms.add(selectedText);
            }

            // get language of the query term
            String language = "en";
            Iterator<Triple> lanIt = queryTermMetadata.filter(null, Properties.DC_LANGUAGE, null);
            if (lanIt.hasNext()) {
                Resource r = lanIt.next().getObject();
                if (r instanceof Literal) {
                    language = ((Literal) r).getLexicalForm();
                } else {
                    language = r.toString();
                }
            }
            /*
             * If there is no stopword list for the language detected, it is highly possible that the default
             * language is detected is false. As English is the most common language, it is set as default.
             */
            if (!stopWords.containsKey(language)) {
                language = "en";
            }

            // eliminate entity query tokens from the original query term
            for (String queryToken : tokenizedTerms) {
                queryTerm = removeQueryToken(queryTerm, queryToken);
            }

            // find non-entity query tokens
            tokenizedTerms.addAll(getNonEntityQueryTerms(queryTerm, language));
        }
        return tokenizedTerms;
    }

    private Set<String> getNonEntityQueryTerms(String queryTerm, String language) {
        String currentWord = "";
        Set<String> queryTokens = new HashSet<String>();
        List<String> languageSpecificStopWords = stopWords.get(language);
        for (int i = 0; i < queryTerm.length(); i++) {
            if (SolrQueryUtil.queryDelimiters.contains(queryTerm.charAt(i))) {
                if (!currentWord.equals("")) {
                    if (languageSpecificStopWords != null) {
                        if (!languageSpecificStopWords.contains(currentWord.trim().toLowerCase())) {
                            queryTokens.add(currentWord);
                        }
                    } else {
                        queryTokens.add(currentWord);
                    }
                }
                currentWord = "";
                continue;
            }
            currentWord += queryTerm.charAt(i);
        }
        // check for the last word
        if (!currentWord.equals("")) {
            if (languageSpecificStopWords != null) {
                if (!languageSpecificStopWords.contains(currentWord.trim().toLowerCase())) {
                    queryTokens.add(currentWord);
                }
            } else {
                queryTokens.add(currentWord);
            }
        }
        return queryTokens;
    }

    private String removeQueryToken(String queryTerm, String queryToken) {
        String newTerm;
        int tokenStartIndex = queryTerm.indexOf(queryToken);
        if (tokenStartIndex != -1) {
            // find right delimeter
            int rightDelimeterIndex = tokenStartIndex + queryToken.length();
            for (; rightDelimeterIndex < queryTerm.length(); rightDelimeterIndex++) {
                if (SolrQueryUtil.queryDelimiters.contains(queryTerm.charAt(rightDelimeterIndex))) {
                    rightDelimeterIndex++;
                    break;
                }
            }
            newTerm = queryTerm.substring(0, tokenStartIndex);
            if (rightDelimeterIndex < queryTerm.length()) {
                newTerm += queryTerm.substring(rightDelimeterIndex);
            }
        } else {
            newTerm = queryTerm;
        }
        return newTerm;
    }
}