/*
 * Copyright (C) 2022 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 */

package au.org.ala.bie

import com.stehno.ersatz.ContentType
import com.stehno.ersatz.Encoders
import com.stehno.ersatz.ErsatzServer
import grails.testing.services.ServiceUnitTest
import groovy.json.JsonSlurper
import org.owasp.html.HtmlPolicyBuilder
import org.owasp.html.PolicyFactory
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.util.regex.Pattern

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
class ExternalSiteServiceSpec extends Specification implements ServiceUnitTest<ExternalSiteService> {
    @AutoCleanup
    ErsatzServer server

    PolicyFactory policy

    def setup() {
        server = new ErsatzServer()
        server.reportToConsole()
        service.setConfiguration(grailsApplication.config)

        String allowedElements = "h2,div,a,br,i,b,span,ul,li,p,sup"
        String allowedAttributes ="href;a;^(http|https|mailto|#).+,class;span,id;span,src;img;^(http|https).+"

        HtmlPolicyBuilder builder = new HtmlPolicyBuilder()
                .allowStandardUrlProtocols()
                .requireRelNofollowOnLinks()

        if (allowedElements) {
            String[] elements = allowedElements.split(",")
            elements.each {
                builder.allowElements(it)
            }
        }

        if (allowedAttributes){
            String[] attributes = allowedAttributes.split(",")
            attributes.each { attribute ->
                String[] values = attribute.split (";")
                if (values.length == 2){
                    builder.allowAttributes(values[0]).onElements(values[1])
                } else {
                    builder.allowAttributes(values[0]).matching(Pattern.compile(values[2], Pattern.CASE_INSENSITIVE)).onElements(values[1])
                }

            }
        }

        policy = builder.toFactory()
    }

    def cleanup() {
    }

    protected Map getResponse(String resource) {
        JsonSlurper slurper = new JsonSlurper()
        return slurper.parse(this.class.getResource(resource), 'UTF-8')
    }


    void "test get BHL literature"() {
        given:
        server.expectations {
            get('/api3') {
                query('op', 'PublicationSearch')
                query('searchterm', '\"Acacia dealbata\"')
                query('page', '1')
                query('apikey', '<key value>')
                query('format', 'json')
                called(1)
                responder {
                    encoder(ContentType.APPLICATION_JSON, Map, Encoders.json)
                    code(200)
                    body(getResponse('bhl-search-1.json'), ContentType.APPLICATION_JSON)
                }
            }

        }
        when:
        service.bhlApi = server.httpUrl + '/api3'
        def response = service.searchBhl(['Acacia dealbata'], 0, 10, false)
        then:
        response != null
        response.max == 2
        response.more == false
        response.start == 0
        response.rows == 10
        response.search != null
        response.search in List
        response.search.size() == 1
        response.results != null
        response.results in List
        response.results.size() == 2
        def result = response.results[0]
        result.type == 'article-journal'
        result.title == 'Allelopathic effect of the invasive Acacia dealbata Link (Fabaceae) on two native plant species in south-central Chile'
    }

    void "test EOL language detection on non-mixed content"(){
        given :
        def englishContent = "<div> <p> </p> <p>The <b>swamp wallaby</b> (<i>Wallabia bicolor</i>) is a small <a href=\"http://en.wikipedia.org/wiki/Macropodidae\" title=\"Macropodidae\">macropod</a> <a href=\"http://en.wikipedia.org/wiki/Marsupial\" title=\"Marsupial\">marsupial</a> of eastern Australia.<sup><a href=\"#cite_note-Merchant1995-3\">[3]</a></sup> This <a href=\"http://en.wikipedia.org/wiki/Wallaby\" title=\"Wallaby\">wallaby</a> is also commonly known as the <b>black wallaby</b>, with other names including <b>black-tailed wallaby</b>, <b>fern wallaby</b>, <b>black <a href=\"http://en.wikipedia.org/wiki/Pademelon\" title=\"Pademelon\">pademelon</a></b>, <b>stinker</b> (in <a href=\"http://en.wikipedia.org/wiki/Queensland\" title=\"Queensland\">Queensland</a>), and <b>black stinker</b> (in <a href=\"http://en.wikipedia.org/wiki/New_South_Wales\" title=\"New South Wales\">New South Wales</a>) on account of its characteristic swampy odour. </p>\n<p>The swamp wallaby is the <a href=\"http://en.wikipedia.org/wiki/Monotypic_taxon\" title=\"Monotypic taxon\">only living member</a> of the genus <i><b>Wallabia</b></i>.<sup><a href=\"#cite_note-Merchant1983-4\">[4]</a></sup> </p> <div>\n<div>\n<h2>Contents</h2>\n<span></span>\n</div> <ul> <li><a href=\"#Etymology\"><span>1</span> <span>Etymology</span></a></li> <li><a href=\"#Habitat_and_distribution\"><span>2</span> <span>Habitat and distribution</span></a></li> <li><a href=\"#Description\"><span>3</span> <span>Description</span></a></li> <li><a href=\"#Reproduction\"><span>4</span> <span>Reproduction</span></a></li> <li><a href=\"#Nutrition\"><span>5</span> <span>Nutrition</span></a></li> <li><a href=\"#Taxonomy\"><span>6</span> <span>Taxonomy</span></a></li> <li><a href=\"#Threats\"><span>7</span> <span>Threats</span></a></li> <li><a href=\"#References\"><span>8</span> <span>References</span></a></li> <li><a href=\"#External_links\"><span>9</span> <span>External links</span></a></li> </ul> </div> <h2><span>Etymology</span></h2> <p>Historic names for the swamp wallaby include <b>Aroe kangaroo</b> and <i><b>Macropus ualabatus</b></i>, as well as <i>banggarai</i> in the <a href=\"http://en.wikipedia.org/wiki/Dharawal_language\" title=\"Dharawal language\">Dharawal language</a>.<sup><a href=\"#cite_note-5\">[5]</a></sup> </p> <h2><span>Habitat and distribution</span></h2> <p>The swamp wallaby is found from the northernmost areas of <a href=\"http://en.wikipedia.org/wiki/Cape_York_Peninsula\" title=\"Cape York Peninsula\">Cape York Peninsula</a> in <a href=\"http://en.wikipedia.org/wiki/Queensland\" title=\"Queensland\">Queensland</a>, down the entire east coast and around to southwestern <a href=\"http://en.wikipedia.org/wiki/Victoria_(Australia)\" title=\"Victoria (Australia)\">Victoria</a>. It was formerly found throughout southeastern <a href=\"http://en.wikipedia.org/wiki/South_Australia\" title=\"South Australia\">South Australia</a>, but is now rare or absent from that region.<sup><a href=\"#cite_note-Merchant1983-4\">[4]</a></sup> </p>\n<p>It inhabits thick undergrowth in <a href=\"http://en.wikipedia.org/wiki/Forest\" title=\"Forest\">forests</a> and <a href=\"http://en.wikipedia.org/wiki/Woodland\" title=\"Woodland\">woodlands</a>, or shelters during the day in thick <a href=\"http://en.wikipedia.org/wiki/Grass\" title=\"Grass\">grass</a> or <a href=\"http://en.wikipedia.org/wiki/Fern\" title=\"Fern\">ferns</a>, emerging at night to feed. <a href=\"http://en.wikipedia.org/wiki/Acacia_harpophylla\" title=\"Acacia harpophylla\">Brigalow scrub</a> in Queensland is a particularly favoured <a href=\"http://en.wikipedia.org/wiki/Habitat\" title=\"Habitat\">habitat</a>.<sup><a href=\"#cite_note-Merchant1983-4\">[4]</a></sup> </p> <div><div>\n<a href=\"http://en.wikipedia.org/wiki/File:Wallabia_bicolor_Jenolan_Caves_portrait.jpg\"><img alt=\" src=\" width=\"220\" height=\"164\"></a> <div>\n<div><a href=\"http://en.wikipedia.org/wiki/File:Wallabia_bicolor_Jenolan_Caves_portrait.jpg\" title=\"Enlarge\"></a></div>Note the light cheek stripe</div>\n</div></div> <div><div>\n<a href=\"http://en.wikipedia.org/wiki/File:Swamp_wallaby_joey.jpg\"><img alt=\" src=\" width=\"220\" height=\"147\"></a> <div>\n<div><a href=\"http://en.wikipedia.org/wiki/File:Swamp_wallaby_joey.jpg\" title=\"Enlarge\"></a></div>A \"pinky\" stage pouch joey</div>\n</div></div> <div><div>\n<a href=\"http://en.wikipedia.org/wiki/File:Swamp-Wallaby-joey-Wallabia-bicolor-cropped.jpg\"><img alt=\" src=\" width=\"220\" height=\"147\"></a> <div>\n<div><a href=\"http://en.wikipedia.org/wiki/File:Swamp-Wallaby-joey-Wallabia-bicolor-cropped.jpg\" title=\"Enlarge\"></a></div>A \"young at foot\" joey</div>\n</div></div> <h2><span>Description</span></h2> <p>The species name <i>bicolor</i> comes from the distinct colouring variation, with the typical grey coat of the macropods varied with a dark brown to black region on the back, and light yellow to rufous orange on the chest. A light coloured cheek stripe is usually present, and extremities of the body generally show a darker colouring, except for the tip of the <a href=\"http://en.wikipedia.org/wiki/Tail\" title=\"Tail\">tail</a>, which is often white.<sup><a href=\"#cite_note-Merchant1995-3\">[3]</a></sup> </p>\n<p>The gait differs from other wallabies, with the swamp wallaby carrying its head low and its tail out straight.<sup><a href=\"#cite_note-Merchant1983-4\">[4]</a></sup> </p>\n<p>The average length is 76 cm (30 in) for males, and 70 cm (28 in) for females (excluding the tail). The tail in both sexes is approximately equal in length to the rest of the body. Average weight for males is 17 kg (37 lb), females averaging 13 kg (29 lb).<sup><a href=\"#cite_note-Merchant1983-4\">[4]</a></sup> </p>\n<p>The swamp wallaby has seven carpal bones in the wrist (humans have eight).<sup><a href=\"#cite_note-6\">[6]</a></sup> </p> <h2><span>Reproduction</span></h2> <p>The swamp wallaby becomes reproductively fertile between 15 and 18 months of age, and can breed throughout the year. <a href=\"http://en.wikipedia.org/wiki/Gestation\" title=\"Gestation\">Gestation</a> is from 33 to 38 days, leading to a single young. The young is carried in the <a href=\"http://en.wikipedia.org/wiki/Pouch_(marsupial)\" title=\"Pouch (marsupial)\">pouch</a> for 8 to 9 months, but will continue to <a href=\"http://en.wikipedia.org/wiki/Breastfeeding\" title=\"Breastfeeding\">suckle</a> until about 15 months. </p>\n<p>The swamp wallaby exhibits an unusual form of <a href=\"http://en.wikipedia.org/wiki/Embryonic_diapause\" title=\"Embryonic diapause\">embryonic diapause</a>, differing from other marsupials in having its gestation period longer than its <a href=\"http://en.wikipedia.org/wiki/Estrous_cycle\" title=\"Estrous cycle\">oestrous cycle</a>.<sup><a href=\"#cite_note-Merchant1983-4\">[4]</a></sup> This timing makes it possible for swamp wallaby females to overlap two pregnancies, gestating both an embryo and a fetus at the same time. The swamp wallaby ovulates, mates, conceives and forms a new embryo one to two days before the birth of their full-term fetus. Consequently, females are continuously pregnant throughout their reproductive life.<sup><a href=\"#cite_note-7\">[7]</a></sup> </p>\n<p>The swamp wallaby is notable for having a distinct sex-chromosome system from most other <a href=\"http://en.wikipedia.org/wiki/Therian_mammals\" title=\"Therian mammals\">Theria</a> (the subclass that includes <a href=\"http://en.wikipedia.org/wiki/Marsupials\" title=\"Marsupials\">marsupials</a> and <a href=\"http://en.wikipedia.org/wiki/Placental_mammals\" title=\"Placental mammals\">placental mammals</a>). Females are characterized by the XX pair typical of therians, but males have one <a href=\"http://en.wikipedia.org/wiki/X_chromosome\" title=\"X chromosome\">X chromosome</a> and two non-<a href=\"http://en.wikipedia.org/wiki/Homologous\" title=\"Homologous\">sequence homology</a> <a href=\"http://en.wikipedia.org/wiki/Y_chromosomes\" title=\"Y chromosomes\">Y chromosomes</a>. This system is thought to arise from a series of chromosomal fusions over the last 6 million years. <sup><a href=\"#cite_note-8\">[8]</a></sup> </p> <div><div>\n<a href=\"http://en.wikipedia.org/wiki/File:Image-Swamp-Wallaby-Feeding-4,-Vic,-Jan.2008.jpg\"><img alt=\" src=\" width=\"220\" height=\"165\"></a> <div>\n<div><a href=\"http://en.wikipedia.org/wiki/File:Image-Swamp-Wallaby-Feeding-4,-Vic,-Jan.2008.jpg\" title=\"Enlarge\"></a></div>A swamp wallaby feeding on leaves</div>\n</div></div> <h2><span>Nutrition</span></h2> <p>The swamp wallaby is typically a <a href=\"https://en.wiktionary.org/wiki/solitary\" title=\"wikt:solitary\">solitary</a> animal, but often aggregates into groups when feeding.<sup><a href=\"#cite_note-Merchant1995-3\">[3]</a></sup> It will eat a wide range of <a href=\"http://en.wikipedia.org/wiki/Food\" title=\"Food\">food</a> <a href=\"http://en.wikipedia.org/wiki/Plant\" title=\"Plant\">plants</a>, depending on availability, including <a href=\"http://en.wikipedia.org/wiki/Shrub\" title=\"Shrub\">shrubs</a>, <a href=\"http://en.wikipedia.org/wiki/Pasture\" title=\"Pasture\">pasture</a>, <a href=\"http://en.wikipedia.org/wiki/Agriculture\" title=\"Agriculture\">agricultural</a> <a href=\"http://en.wikipedia.org/wiki/Crop\" title=\"Crop\">crops</a>, and <a href=\"http://en.wikipedia.org/wiki/Indigenous_(ecology)\" title=\"Indigenous (ecology)\">native</a> and <a href=\"http://en.wikipedia.org/wiki/Introduced_species\" title=\"Introduced species\">exotic</a> <a href=\"http://en.wikipedia.org/wiki/Vegetation\" title=\"Vegetation\">vegetation</a>. It appears to be able to tolerate a variety of plants poisonous to many other animals, including <a href=\"http://en.wikipedia.org/wiki/Bracken\" title=\"Bracken\">brackens</a>, <a href=\"http://en.wikipedia.org/wiki/Conium\" title=\"Conium\">hemlock</a> and <a href=\"http://en.wikipedia.org/wiki/Lantana\" title=\"Lantana\">lantana</a>.<sup><a href=\"#cite_note-Merchant1983-4\">[4]</a></sup> </p>\n<p>The ideal diet appears to involve <a href=\"http://en.wikipedia.org/wiki/Browsing_(predation)\" title=\"Browsing (predation)\">browsing</a> on <a href=\"http://en.wikipedia.org/wiki/Shrub\" title=\"Shrub\">shrubs</a> and bushes, rather than <a href=\"http://en.wikipedia.org/wiki/Grazing\" title=\"Grazing\">grazing</a> on grasses. This is unusual in wallabies and other macropods, which typically prefer grazing. <a href=\"http://en.wikipedia.org/wiki/Tooth\" title=\"Tooth\">Tooth</a> structure reflects this preference for browsing, with the shape of the <a href=\"http://en.wikipedia.org/wiki/Molar_(tooth)\" title=\"Molar (tooth)\">molars</a> differing from other wallabies. The fourth <a href=\"http://en.wikipedia.org/wiki/Premolar\" title=\"Premolar\">premolar</a> is retained through life, and is shaped for cutting through coarse plant material.<sup><a href=\"#cite_note-Merchant1983-4\">[4]</a></sup> </p>\n<p>There is evidence that the swamp wallaby is an opportunist taking advantage of food sources when they become available, such as fungi, bark and algae. There is also one reported case of the consumption of carrion.<sup><a href=\"#cite_note-9\">[9]</a></sup> </p> <h2><span>Taxonomy</span></h2> <p>Several physical and behavioral characteristics make the swamp wallaby different enough from other wallabies that it is placed apart in its own genus, <i>Wallabia</i>.<sup><a href=\"#cite_note-msw3-10\">[10]</a></sup><sup><a href=\"#cite_note-Merchant1995-3\">[3]</a></sup> However, genetic evidence (e.g. Dodt <i>et al</i>, 2017) demonstrates that <i>Wallabia</i> is embedded within the large genus <i>Macropus</i>, necessitating reclassification of this species in the future. </p>\n<p>According to the <a href=\"http://en.wikipedia.org/wiki/Indigenous_Australians\" title=\"Indigenous Australians\">Aboriginal people</a> of the <a href=\"http://en.wikipedia.org/wiki/List_of_Indigenous_Australian_group_names#B\" title=\"List of Indigenous Australian group names\">Bundjalung</a> Nation, the swamp wallaby was considered inedible, due to its smell and taste after cooking. Commercial <a href=\"http://en.wikipedia.org/wiki/Shooting\" title=\"Shooting\">shooters</a> also find it undesirable due to its small size and coarse fur.<sup><a href=\"#cite_note-Merchant1983-4\">[4]</a></sup> </p> <h2><span>Threats</span></h2> <p>Anthropogenic actions, such as the increase in roads through swamp wallaby habitats, are a threat to their survival. They are frequently seen near the side of roads, leading to a larger number becoming <a href=\"http://en.wikipedia.org/wiki/Roadkill\" title=\"Roadkill\">roadkill</a>.<sup><a href=\"#cite_note-11\">[11]</a></sup> </p>\n<p>Other sources of threat for the swamp wallaby are their predators, which include dingoes, eagles and wild dogs.<sup><a href=\"#cite_note-12\">[12]</a></sup> </p> <h2><span>References</span></h2> <div> <div><ol> <li>\n<span><b><a href=\"#cite_ref-iucn_status_12_November_2021_1-0\">^</a></b></span> <span><cite>Menkhorst, P.; Denny, M.; Ellis, M.; Winter, J.; Burnett, S.; Lunney, D.; van Weenen, J. (2016). <a href=\"https://www.iucnredlist.org/species/40575/21952658\">\"<i>Wallabia bicolor</i>\"</a>. <i><a href=\"http://en.wikipedia.org/wiki/IUCN_Red_List\" title=\"IUCN Red List\">IUCN Red List of Threatened Species</a></i>. <b>2016</b>: e.T40575A21952658. <a href=\"http://en.wikipedia.org/wiki/Doi_(identifier)\" title=\"Doi (identifier)\">doi</a>:<span title=\"Freely accessible\"><a href=\"https://doi.org/10.2305%2FIUCN.UK.2016-2.RLTS.T40575A21952658.en\">10.2305/IUCN.UK.2016-2.RLTS.T40575A21952658.en</a></span><span>. Retrieved <span>12 November</span> 2021</span>.</cite></span> </li> <li>\n<span><b><a href=\"#cite_ref-2\">^</a></b></span> <span><cite>Trouessart, E.-L. (1904). <a href=\"https://www.biodiversitylibrary.org/page/40579370\"><i>Catalogus mammalium tam viventium quam fossilium</i></a>. Vol. Quinquennale supplementum. Berolini: R. Friedländer &amp; Sohn. p. 834.</cite></span> </li> <li>\n<span>^ <a href=\"#cite_ref-Merchant1995_3-0\"><sup><i><b>a</b></i></sup></a> <a href=\"#cite_ref-Merchant1995_3-1\"><sup><i><b>b</b></i></sup></a> <a href=\"#cite_ref-Merchant1995_3-2\"><sup><i><b>c</b></i></sup></a> <a href=\"#cite_ref-Merchant1995_3-3\"><sup><i><b>d</b></i></sup></a></span> <span><cite>Merchant, J. C. (1995). Strahan, Ronald (ed.). <i>Mammals of Australia</i> (Revised ed.). Sydney: Reed New Holland Publishers. p. 409.</cite></span> </li> <li>\n<span>^ <a href=\"#cite_ref-Merchant1983_4-0\"><sup><i><b>a</b></i></sup></a> <a href=\"#cite_ref-Merchant1983_4-1\"><sup><i><b>b</b></i></sup></a> <a href=\"#cite_ref-Merchant1983_4-2\"><sup><i><b>c</b></i></sup></a> <a href=\"#cite_ref-Merchant1983_4-3\"><sup><i><b>d</b></i></sup></a> <a href=\"#cite_ref-Merchant1983_4-4\"><sup><i><b>e</b></i></sup></a> <a href=\"#cite_ref-Merchant1983_4-5\"><sup><i><b>f</b></i></sup></a> <a href=\"#cite_ref-Merchant1983_4-6\"><sup><i><b>g</b></i></sup></a> <a href=\"#cite_ref-Merchant1983_4-7\"><sup><i><b>h</b></i></sup></a> <a href=\"#cite_ref-Merchant1983_4-8\"><sup><i><b>i</b></i></sup></a></span> <span><cite>Merchant, J. C. (1983). Strahan, Ronald (ed.). <i>The Australian Museum Complete Book of Australian Mammals, The National Photographic Index of Australian Wildlife</i> (Corrected 1991 reprint ed.). Australia: Cornstalk Publishing. pp. 261–262. <a href=\"http://en.wikipedia.org/wiki/ISBN_(identifier)\" title=\"ISBN (identifier)\">ISBN</a> <a href=\"http://en.wikipedia.org/wiki/Special:BookSources/0-207-14454-0\" title=\"Special:BookSources/0-207-14454-0\">0-207-14454-0</a>.</cite></span> </li> <li>\n<span><b><a href=\"#cite_ref-5\">^</a></b></span> <span><cite><a href=\"https://dharug.dalang.com.au/language/view_word/1855\">\"Dharug and Dharawal Resources\"</a>.</cite></span> </li> <li>\n<span><b><a href=\"#cite_ref-6\">^</a></b></span> <span><cite><a href=\"https://kmccready.wordpress.com/2014/07/03/carpals-of-swamp-wallaby-wallabia-bicolor/\">\"Carpals of Swamp Wallaby – Wallabia bicolor\"</a>. 3 July 2014.</cite></span> </li> <li>\n<span><b><a href=\"#cite_ref-7\">^</a></b></span> <span><cite><a href=\"https://www.sciencedaily.com/releases/2020/03/200302153611.htm\">\"Swamp wallabies conceive new embryo before birth -- a unique reproductive strategy\"</a>. <i>ScienceDaily</i><span>. Retrieved <span>2020-03-03</span></span>.</cite></span> </li> <li>\n<span><b><a href=\"#cite_ref-8\">^</a></b></span> <span><cite>Toder, R; O'Neill, R J; Wienberg, K; O'Brien, P C; Voullaire, L; Marshall-Graves, J A (June 1997). <a href=\"https://pubmed.ncbi.nlm.nih.gov/9166586/\">\"Comparative chromosome painting between two marsupials: origins of an XX/XY1Y2 sex chromosome system\"</a>. <i>Mamm Genome</i>. <b>8</b> (6): 418–22. <a href=\"http://en.wikipedia.org/wiki/Doi_(identifier)\" title=\"Doi (identifier)\">doi</a>:<a href=\"https://doi.org/10.1007%2Fs003359900459\">10.1007/s003359900459</a>. <a href=\"http://en.wikipedia.org/wiki/PMID_(identifier)\" title=\"PMID (identifier)\">PMID</a> <a href=\"http://pubmed.ncbi.nlm.nih.gov/9166586\">9166586</a><span>. Retrieved <span>March 1,</span> 2022</span>.</cite></span> </li> <li>\n<span><b><a href=\"#cite_ref-9\">^</a></b></span> <span><cite>Fitzsimons, James A. (2016). \"Carrion consumption by the swamp wallaby (Wallabia bicolor)\". <i>Australian Mammalogy</i>. <b>39</b>: 105. <a href=\"http://en.wikipedia.org/wiki/Doi_(identifier)\" title=\"Doi (identifier)\">doi</a>:<a href=\"https://doi.org/10.1071%2FAM16017\">10.1071/AM16017</a>.</cite></span> </li> <li>\n<span><b><a href=\"#cite_ref-msw3_10-0\">^</a></b></span> <span><cite><a href=\"http://en.wikipedia.org/wiki/Colin_Groves\" title=\"Colin Groves\">Groves, C. P.</a> (2005). <a href=\"http://en.wikipedia.org/wiki/Don_E._Wilson\" title=\"Don E. Wilson\">Wilson, D. E.</a>; Reeder, D. M. (eds.). <a href=\"http://www.departments.bucknell.edu/biology/resources/msw3/browse.asp?id=11000314\"><i>Mammal Species of the World: A Taxonomic and Geographic Reference</i></a> (3rd ed.). Baltimore: Johns Hopkins University Press. p. 70. <a href=\"http://en.wikipedia.org/wiki/ISBN_(identifier)\" title=\"ISBN (identifier)\">ISBN</a> <a href=\"http://en.wikipedia.org/wiki/Special:BookSources/0-801-88221-4\" title=\"Special:BookSources/0-801-88221-4\">0-801-88221-4</a>. <a href=\"http://en.wikipedia.org/wiki/OCLC_(identifier)\" title=\"OCLC (identifier)\">OCLC</a> <a href=\"http://www.worldcat.org/oclc/62265494\">62265494</a>.</cite></span> </li> <li>\n<span><b><a href=\"#cite_ref-11\">^</a></b></span> <span><cite>Osawa, R (1989). <a href=\"http://www.publish.csiro.au/?paper=WR9890095\">\"Road-Kills of the Swamp Wallaby, Wallabia-Bicolor, on North-Stradbroke-Island, Southeast Queensland\"</a>. <i>Wildlife Research</i>. <b>16</b> (1): 95. <a href=\"http://en.wikipedia.org/wiki/Doi_(identifier)\" title=\"Doi (identifier)\">doi</a>:<a href=\"https://doi.org/10.1071%2FWR9890095\">10.1071/WR9890095</a>. <a href=\"http://en.wikipedia.org/wiki/ISSN_(identifier)\" title=\"ISSN (identifier)\">ISSN</a> <a href=\"http://www.worldcat.org/issn/1035-3712\">1035-3712</a>.</cite></span> </li> <li>\n<span><b><a href=\"#cite_ref-12\">^</a></b></span> <span><cite>Davis, Naomi E.; Forsyth, David M.; Triggs, Barbara; Pascoe, Charlie; Benshemesh, Joe; Robley, Alan; Lawrence, Jenny; Ritchie, Euan G.; Nimmo, Dale G.; Lumsden, Lindy F. (2015-03-19). Crowther, Mathew S. (ed.). <a href=\"http://www.ncbi.nlm.nih.gov/pmc/articles/PMC4366095\">\"Interspecific and Geographic Variation in the Diets of Sympatric Carnivores: Dingoes/Wild Dogs and Red Foxes in South-Eastern Australia\"</a>. <i>PLOS ONE</i>. <b>10</b> (3): e0120975. <a href=\"http://en.wikipedia.org/wiki/Bibcode_(identifier)\" title=\"Bibcode (identifier)\">Bibcode</a>:<a href=\"https://ui.adsabs.harvard.edu/abs/2015PLoSO..1020975D\">2015PLoSO..1020975D</a>. <a href=\"http://en.wikipedia.org/wiki/Doi_(identifier)\" title=\"Doi (identifier)\">doi</a>:<span title=\"Freely accessible\"><a href=\"https://doi.org/10.1371%2Fjournal.pone.0120975\">10.1371/journal.pone.0120975</a></span>. <a href=\"http://en.wikipedia.org/wiki/ISSN_(identifier)\" title=\"ISSN (identifier)\">ISSN</a> <a href=\"http://www.worldcat.org/issn/1932-6203\">1932-6203</a>. <a href=\"http://en.wikipedia.org/wiki/PMC_(identifier)\" title=\"PMC (identifier)\">PMC</a> <span title=\"Freely accessible\"><a href=\"http://www.ncbi.nlm.nih.gov/pmc/articles/PMC4366095\">4366095</a></span>. <a href=\"http://en.wikipedia.org/wiki/PMID_(identifier)\" title=\"PMID (identifier)\">PMID</a> <a href=\"http://pubmed.ncbi.nlm.nih.gov/25790230\">25790230</a>.</cite></span> </li> </ol></div>\n</div> <h2> </h2>\n</div><img src=\"https://en.wikipedia.org/wiki/Special:CentralAutoLogin/start?type=1x1\" alt=\" title=\" width=\"1\" height=\"1\">"
        def frenchContent = "<p>Wallabia bicolor</p> <p>Le wallaby bicolore (Wallabia bicolor) est un <a href=\"http://fr.wikipedia.org/wiki/Marsupial\" title=\"Marsupial\">marsupial</a> appartenant à la <a href=\"http://fr.wikipedia.org/wiki/Famille_(biologie)\" title=\"Famille (biologie)\">famille</a> des <a href=\"http://fr.wikipedia.org/wiki/Macropodid%C3%A9s\" title=\"Macropodidés\">macropodidés</a>, laquelle inclut en particulier les <a href=\"http://fr.wikipedia.org/wiki/Kangourou\" title=\"Kangourou\">kangourous</a> et les différentes espèces de <a href=\"http://fr.wikipedia.org/wiki/Wallaby\" title=\"Wallaby\">wallabys</a>, originaire de la côte est de l'<a href=\"http://fr.wikipedia.org/wiki/Australie\" title=\"Australie\">Australie</a>. </p><p>Wallabia bicolor est la seule <a href=\"http://fr.wikipedia.org/wiki/Esp%C3%A8ce\" title=\"Espèce\">espèce</a> du <a href=\"http://fr.wikipedia.org/wiki/Genre_(biologie)\" title=\"Genre (biologie)\">genre</a> Wallabia. </p>"
        when:
        def isEnglish = service.isContentInLanguage(englishContent, "en")
        def isFrench = service.isContentInLanguage(frenchContent, "fr")
        then:
        isEnglish
        isFrench
    }

    void "test EOL language detection on mixed content"(){
        given :
        //  content extracted EOL search responses e.g taxonConcept.dataObjects[0].description from https://eol.org/api/pages/1.0/128424.json?language=en&images_per_page=0&videos_per_page=0&sounds_per_page=0&maps_per_page=0&texts_per_page=30&subjects=overview&licenses=all&details=true&references=true&vetted=0&cache_ttl=
        def koreanContent1 = "<p>왈라비아 또는 늪왈라비(Wallabia bicolor)는 <a href=\"http://ko.wikipedia.org/wiki/%EC%BA%A5%EA%B1%B0%EB%A3%A8%EA%B3%BC\" title=\"캥거루과\">캥거루과</a>에 속하는 작은 <a href=\"http://ko.wikipedia.org/wiki/%EC%9C%A0%EB%8C%80%EB%A5%98\" title=\"유대류\">유대류</a>의 일종이다. <a href=\"http://ko.wikipedia.org/wiki/%EC%98%A4%EC%8A%A4%ED%8A%B8%EB%A0%88%EC%9D%BC%EB%A6%AC%EC%95%84\" title=\"오스트레일리아\">오스트레일리아</a> 동부 지역에서 발견된다. 이 <a href=\"http://ko.wikipedia.org/wiki/%EC%99%88%EB%9D%BC%EB%B9%84\" title=\"왈라비\">왈라비</a>는 \"검은꼬리왈라비\"(black-tailed wallaby), \"검은덤불왈라비\"(black pademelon)라는 이름을 포함하여 검은왈라비(black wallaby)라는 이름으로 흔히 알려져 있으며, 늪에서 나는 냄새가 나기 때문에 \"스팅커\"(stinker, <a href=\"http://ko.wikipedia.org/wiki/%ED%80%B8%EC%A6%90%EB%9E%9C%EB%93%9C%EC%A3%BC\" title=\"퀸즐랜드주\">퀸즐랜드주</a>에서), \"검은스팅커\"(black stinker, <a href=\"http://ko.wikipedia.org/wiki/%EB%89%B4%EC%82%AC%EC%9A%B0%EC%8A%A4%EC%9B%A8%EC%9D%BC%EC%8A%A4%EC%A3%BC\" title=\"뉴사우스웨일스주\">뉴사우스웨일스주</a>)라는 이름으로도 불린다. 왈라비아속(Wallabia)의 유일종이다. </p>"
        def koreanContent2 = "<div> <p><b>붉은쥐캥거루</b>(<i>Aepyprymnus rufescens</i>)은 <a href=\"http://ko.wikipedia.org/wiki/%EC%A5%90%EC%BA%A5%EA%B1%B0%EB%A3%A8\" title=\"쥐캥거루\">쥐캥거루과</a>에 속하는 작은 유대류 종의 하나로 <a href=\"http://ko.wikipedia.org/wiki/%EC%98%A4%EC%8A%A4%ED%8A%B8%EB%A0%88%EC%9D%BC%EB%A6%AC%EC%95%84\" title=\"오스트레일리아\">오스트레일리아</a>에서 발견된다. <b>붉은베통</b>(rufous bettong)으로도 알려져 있다. <a href=\"http://ko.wikipedia.org/wiki/%EB%89%B4%EC%82%AC%EC%9A%B0%EC%8A%A4%EC%9B%A8%EC%9D%BC%EC%8A%A4%EC%A3%BC\" title=\"뉴사우스웨일스주\">뉴사우스웨일스주</a>의 <a href=\"http://ko.wikipedia.org/wiki/%EB%89%B4%EC%BA%90%EC%8A%AC\" title=\"뉴캐슬\">뉴캐슬</a> 지역부터 <a href=\"http://ko.wikipedia.org/wiki/%ED%80%B8%EC%A6%90%EB%9E%9C%EB%93%9C%EC%A3%BC\" title=\"퀸즐랜드주\">퀸즐랜드주</a>의 쿡타운 지역까지의 해안가와 해안 하부 지역에서 발견되며, 이전에는 뉴사우스웨일스 주와 <a href=\"http://ko.wikipedia.org/wiki/%EB%B9%85%ED%86%A0%EB%A6%AC%EC%95%84%EC%A3%BC\" title=\"빅토리아주\">빅토리아주</a>의 <a href=\"http://ko.wikipedia.org/wiki/%EB%A8%B8%EB%A6%AC_%EA%B0%95\" title=\"머리 강\">머리 강</a> 계곡에서 발견되었다.<sup><a href=\"#cite_note-Menkhorst-3\">[3]</a></sup> </p>\n" +
                "<p>관심대상종으로 분류되고 있다.<sup><a href=\"#cite_note-iucn-2\">[2]</a></sup> 붉은쥐캥거루는 다 자란 회색래빗 정도의 크기이다. <b>붉은쥐캥거루속</b>(<i>Aepyprymnus</i>)의 유일종이며, <a href=\"http://ko.wikipedia.org/wiki/%EC%A5%90%EC%BA%A5%EA%B1%B0%EB%A3%A8\" title=\"쥐캥거루\">쥐캥거루과</a>에서 가장 큰 종이다. 일반적으로 약간 붉은 갈색을 보이는 회색을 띠며, 학명은 \"불그스레한 궁둥이\"(<i>reddish high-rump</i>)라는 의미이다.<sup><a href=\"#cite_note-Strahan-4\">[4]</a></sup> 한때는 홀로 생활하는 야행성 동물로 간주했으나 최근에 관찰된 바에 의하면 느슨한 일부다처제 사회를 형성하는 것으로 보고 있다.<sup><a href=\"#cite_note-Strahan-4\">[4]</a></sup> 주로 덩이줄기와 <a href=\"http://ko.wikipedia.org/wiki/%EA%B7%A0%EA%B3%84\" title=\"균계\">버섯류</a>를 먹지만, 나무의 잎이나 기타 식물을 먹기도 한다.<sup><a href=\"#cite_note-Menkhorst-3\">[3]</a></sup> 암컷은 약 11개월 이후가 되면 성적으로 성숙해지고, 일년 연중 번식을 한다. 수컷은 12개월과 13개월 사이에 성적으로 성숙해진다. 성숙한 암컷은 3주 간격으로 번식을 할 수 있다. 번식에 성공한 경우 임신 기간은 한 달 이하, 약 22~24일 정도이다. 새끼는 태어난 후 육아낭에서 약 16주 동안 지낸다. 육아낭을 떠난 후, 새끼 쥐캥거루는 어미 곁에서 약 7주 정도 머물먀 혼자 힘으로 살아가는 법을 익힌다.<sup><a href=\"#cite_note-Strahan-4\">[4]</a></sup> </p> <h2>\n" +
                "<span></span><span>각주</span>\n" +
                "</h2> <div> <div><ol> <li>\n" +
                "<span><a href=\"#cite_ref-MSW3_1-0\">↑</a></span> <span><cite>Groves, C.P. (2005). <a href=\"http://www.departments.bucknell.edu/biology/resources/msw3/browse.asp?id=11000178\">〈Order Diprotodontia〉 [캥거루목]</a>. Wilson, D.E.; Reeder, D.M. <a href=\"http://www.google.com/books?id=JgAMbNSt8ikC&amp;pg=PA57\">《Mammal Species of the World: A Taxonomic and Geographic Reference》</a> (영어) 3판. 존스 홉킨스 대학교 출판사. 57쪽. <a href=\"http://ko.wikipedia.org/wiki/%EA%B5%AD%EC%A0%9C_%ED%91%9C%EC%A4%80_%EB%8F%84%EC%84%9C_%EB%B2%88%ED%98%B8\" title=\"국제 표준 도서 번호\">ISBN</a> <a href=\"http://ko.wikipedia.org/wiki/%ED%8A%B9%EC%88%98:%EC%B1%85%EC%B0%BE%EA%B8%B0/978-0-8018-8221-0\" title=\"특수:책찾기/978-0-8018-8221-0\">978-0-8018-8221-0</a>. <a href=\"http://ko.wikipedia.org/wiki/%EC%98%A8%EB%9D%BC%EC%9D%B8_%EC%BB%B4%ED%93%A8%ED%84%B0_%EB%8F%84%EC%84%9C%EA%B4%80_%EC%84%BC%ED%84%B0\" title=\"온라인 컴퓨터 도서관 센터\">OCLC</a> <a href=\"http://www.worldcat.org/oclc/62265494\">62265494</a>.</cite></span> </li> <li>\n" +
                "<span>↑ <sup><a href=\"#cite_ref-iucn_2-0\">가</a></sup> <sup><a href=\"#cite_ref-iucn_2-1\">나</a></sup></span> <span><cite>Burnett, S. &amp; Winter, J. (2008). <a href=\"http://www.iucnredlist.org/details/40558\">“<i>Aepyprymnus rufescens</i>”</a>. 《<a href=\"http://ko.wikipedia.org/wiki/IUCN_%EC%A0%81%EC%83%89_%EB%AA%A9%EB%A1%9D\" title=\"IUCN 적색 목록\">멸종 위기 종의 IUCN 적색 목록</a>. 2013.1판》 (영어). <a href=\"http://ko.wikipedia.org/wiki/%EA%B5%AD%EC%A0%9C_%EC%9E%90%EC%97%B0_%EB%B3%B4%EC%A0%84_%EC%97%B0%EB%A7%B9\" title=\"국제 자연 보전 연맹\">국제 자연 보전 연맹</a><span>. 2013년 10월 13일에 확인함</span>.</cite></span> </li> <li>\n" +
                "<span>↑ <sup><a href=\"#cite_ref-Menkhorst_3-0\">가</a></sup> <sup><a href=\"#cite_ref-Menkhorst_3-1\">나</a></sup></span> <span><cite>Menkhorst, Peter (2001). 《A Field Guide to the Mammals of Australia》. Oxford University Press. 100쪽.</cite></span> </li> <li>\n" +
                "<span>↑ <sup><a href=\"#cite_ref-Strahan_4-0\">가</a></sup> <sup><a href=\"#cite_ref-Strahan_4-1\">나</a></sup> <sup><a href=\"#cite_ref-Strahan_4-2\">다</a></sup></span> <span><cite>Strahan, R. (1995). 《The Mammals of Australia: the National Photographic Index of Australian Wildlife》. Reed Books. 758쪽.</cite></span> </li> </ol></div>\n" +
                "</div>\n" +
                "</div>"
        when:
        def isKorean1 = service.isContentInLanguage(koreanContent1, "ko")
        def isKorean2 = service.isContentInLanguage(koreanContent2, "ko")
        then:
        isKorean1
        isKorean2
    }

    void "test suspect attributes"() {
        given:
        def text = 'Some invalid image attribute <a href="http://en.wikipedia.org/wiki/File:1_Acacia_oswaldii_foliage.jpg"><img alt=" src=" width="220" height="230"></a>'
        when:
        def html = service.sanitiseBodyText(policy, text)
        then:
        html == 'Some invalid image attribute <a href="http://en.wikipedia.org/wiki/File:1_Acacia_oswaldii_foliage.jpg" rel="nofollow"></a>'
    }

    void "test disallowed elements"() {
        given:
        def text = 'Some invalid element <h1>h1 content</h1>'
        when:
        def html = service.sanitiseBodyText(policy, text)
        then:
        html == 'Some invalid element h1 content'
    }

    void "test valid html text"() {
        given:
        def text = '<h2>Contents</h2> <span></span></div> <ul> <li><a href=\"#Description\"><span>1</span> <span>Description</span></a></li> <li><a href=\"#Distribution\"><span>2</span> <span>Distribution</span></a></li> <li><a href=\"#Classification\"><span>3</span> <span>Classification</span></a></li> <li><a href=\"#See_also\"><span>4</span> <span>See also</span></a></li> <li><a href=\"#References\"><span>5</span> <span>References</span></a></li> </ul> </div> '
        when:
        def html = service.sanitiseBodyText(policy, text)
        then:
        html == '<h2>Contents</h2>  <ul><li><a href="#Description" rel="nofollow">1 Description</a></li><li><a href="#Distribution" rel="nofollow">2 Distribution</a></li><li><a href="#Classification" rel="nofollow">3 Classification</a></li><li><a href="#See_also" rel="nofollow">4 See also</a></li><li><a href="#References" rel="nofollow">5 References</a></li></ul>  '
    }

}
