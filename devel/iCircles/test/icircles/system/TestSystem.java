package icircles.system;

import icircles.abstractDescription.AbstractDescription;
import icircles.concreteDiagram.*;
import icircles.util.*;

import org.junit.*;
import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;
import java.util.StringTokenizer;
import java.io.*;

public class TestSystem {

    // A pair to hold test data
    public class TestDatum {
        private String description;
        private double checksum;
        public TestDatum(String description, double checksum) {
            this.description = description;
            this.checksum    = checksum;
        }

        public double getChecksum() {
            return checksum;
        }

        public String getDescription() {
            return description;
        }
    }

    TestSystem.TestDatum [] testData = {
                       /*0*/new TestSystem.TestDatum(
                             "{"
                             + "\"ContourSet\"   : [{\"label\" : \"a\"}],"
                             + "\"ZoneSet\"      : [{\"inSet\" : [{\"label\" : \"a\"}]}],"
                             + "\"ShadedZoneSet\": []"
                             + "}"
                             , 80.35747263647977)
    };

    TestSystem.TestDatum [] unconvertedTests = {
                       /*1*/new TestSystem.TestDatum( "a b", 131.7353635695516),
                       /*2*/new TestSystem.TestDatum( "a b c", 197.80358797941003),
                       /*3*/new TestSystem.TestDatum( "ab", 161.405093902864),
                       /*3*/new TestSystem.TestDatum( "ab", 161.405093902864),
                       /*4*/new TestSystem.TestDatum( "a ab", 161.37471388954395),
                       /*4*/new TestSystem.TestDatum( "a ab", 161.37471388954395),
                       /*5*/new TestSystem.TestDatum( "a b ab", 151.7818962901353),
                       /*6*/new TestSystem.TestDatum( "a b ac", 211.81155789055296),
                       /*6*/new TestSystem.TestDatum( "a b ac", 211.81155789055296),
                       /*7*/new TestSystem.TestDatum( "a b c ab", 221.12921720863423),
                       /*8*/new TestSystem.TestDatum( "ab ac", 243.83530568815246),
                       /*8*/new TestSystem.TestDatum( "ab ac", 243.83530568815246),
                       /*9*/new TestSystem.TestDatum( "a ab ac", 243.80523568815246),
                       /*9*/new TestSystem.TestDatum( "a ab ac", 243.80523568815246),
                       /*10*/new TestSystem.TestDatum( "a b ab ac", 243.07882580414355),
                       /*10*/new TestSystem.TestDatum( "a b ab ac", 243.07882580414355),
                       /*11*/new TestSystem.TestDatum( "a b c ab ac", 234.07155278782034),
                       /*12*/new TestSystem.TestDatum( "a bc", 211.84224793584096),
                       /*12*/new TestSystem.TestDatum( "a bc", 211.84224793584096),
                       /*13*/new TestSystem.TestDatum( "a ab bc", 233.95716959964912),
                       /*13*/new TestSystem.TestDatum( "a ab bc", 233.95716959964912),
                       /*14*/new TestSystem.TestDatum( "a b ac bc", 234.10224283310836),
                       /*15*/new TestSystem.TestDatum( "ab ac bc", 340.787995743938),
                       /*15*/new TestSystem.TestDatum( "ab ac bc", 340.787995743938),
                       /*16*/new TestSystem.TestDatum( "a ab ac bc", 335.9971561294182),
                       /*16*/new TestSystem.TestDatum( "a ab ac bc", 335.9971561294182),
                       /*17*/new TestSystem.TestDatum( "a b ab ac bc", 340.7269258259892),
                       /*17*/new TestSystem.TestDatum( "a b ab ac bc", 340.7269258259892),
                       /*18*/new TestSystem.TestDatum( "a b c ab ac bc", 318.62457672968225),
                       /*18*/new TestSystem.TestDatum( "a b c ab ac bc", 318.62457672968225),
                       /*19*/new TestSystem.TestDatum( "abc", 259.5238409068328),
                       /*19*/new TestSystem.TestDatum( "abc", 259.5238409068328),
                       /*20*/new TestSystem.TestDatum( "a abc", 259.4928408615448),
                       /*20*/new TestSystem.TestDatum( "a abc", 259.4928408615448),
                       /*21*/new TestSystem.TestDatum( "a b abc", 248.93941627137593),
                       /*21*/new TestSystem.TestDatum( "a b abc", 248.93941627137593),
                       /*22*/new TestSystem.TestDatum( "a b c abc", 327.41312175282457),
                       /*22*/new TestSystem.TestDatum( "a b c abc", 327.41312175282457),
                       /*23*/new TestSystem.TestDatum( "ab abc", 259.46156986420885),
                       /*23*/new TestSystem.TestDatum( "ab abc", 259.46156986420885),
                       /*24*/new TestSystem.TestDatum( "a ab abc", 259.4311898482248),
                       /*24*/new TestSystem.TestDatum( "a ab abc", 259.4311898482248),
                       /*25*/new TestSystem.TestDatum( "a b ab abc", 248.87808427137594),
                       /*25*/new TestSystem.TestDatum( "a b ab abc", 248.87808427137594),
                       /*26*/new TestSystem.TestDatum( "a b ac abc", 316.32389685016204),
                       /*26*/new TestSystem.TestDatum( "a b ac abc", 316.32389685016204),
                       /*27*/new TestSystem.TestDatum( "a b c ab abc", 327.35116083487577),
                       /*27*/new TestSystem.TestDatum( "a b c ab abc", 327.35116083487577),
                       /*28*/new TestSystem.TestDatum( "ab ac abc", 260.9812518732166),
                       /*28*/new TestSystem.TestDatum( "ab ac abc", 260.9812518732166),
                       /*29*/new TestSystem.TestDatum( "a ab ac abc", 260.95118187321657),
                       /*29*/new TestSystem.TestDatum( "a ab ac abc", 260.95118187321657),
                       /*30*/new TestSystem.TestDatum( "a b ab ac abc", 247.17005943313438),
                       /*31*/new TestSystem.TestDatum( "a b c ab ac abc", 344.7353256172181),
                       /*31*/new TestSystem.TestDatum( "a b c ab ac abc", 344.7353256172181),
                       /*32*/new TestSystem.TestDatum( "a bc abc", 316.3545868379076),
                       /*32*/new TestSystem.TestDatum( "a bc abc", 316.3545868379076),
                       /*33*/new TestSystem.TestDatum( "a ab bc abc", 248.03519043865083),
                       /*34*/new TestSystem.TestDatum( "a b ac bc abc", 344.76601560496374),
                       /*34*/new TestSystem.TestDatum( "a b ac bc abc", 344.76601560496374),
                       /*35*/new TestSystem.TestDatum( "ab ac bc abc", 342.02093349760264),
                       /*35*/new TestSystem.TestDatum( "ab ac bc abc", 342.02093349760264),
                       /*36*/new TestSystem.TestDatum( "a ab ac bc abc", 352.4610746245922),
                       /*36*/new TestSystem.TestDatum( "a ab ac bc abc", 352.4610746245922),
                       /*37*/new TestSystem.TestDatum( "a b ab ac bc abc", 341.95986357965387),
                       /*37*/new TestSystem.TestDatum( "a b ab ac bc abc", 341.95986357965387),
                       /*38*/new TestSystem.TestDatum( "a b c ab ac bc abc", 239.42535182578533),
                       /*39*/new TestSystem.TestDatum( "ab b", 161.374713902864),
                       /*39*/new TestSystem.TestDatum( "ab b", 161.374713902864),
                       /*40*/new TestSystem.TestDatum( "a ab b", 151.7818962901353),
                       /*41*/new TestSystem.TestDatum( "bc a b ", 211.81155791985697),
                       /*41*/new TestSystem.TestDatum( "bc a b ", 211.81155791985697),
                       /*42*/new TestSystem.TestDatum( "a ab c", 217.76153949873623),
                       /*42*/new TestSystem.TestDatum( "a ab c", 217.76153949873623),
                       /*43*/new TestSystem.TestDatum( "a abc abcd", 378.35206382910627),
                       /*43*/new TestSystem.TestDatum( "a abc abcd", 378.35206382910627),
                       /*44*/new TestSystem.TestDatum( "abc b c ab ac bc", 341.95986349760267),
                       /*44*/new TestSystem.TestDatum( "abc b c ab ac bc", 341.95986349760267),
                       /*45*/new TestSystem.TestDatum( "a b c ab ac bc", 318.62457672968225),
                       /*45*/new TestSystem.TestDatum( "a b c ab ac bc", 318.62457672968225),
                       /*46*/new TestSystem.TestDatum( "a b c ab ac abc", 344.7353256172181),
                       /*46*/new TestSystem.TestDatum( "a b c ab ac abc", 344.7353256172181),
                       /*47*/new TestSystem.TestDatum( "a b ab ac bc abc", 341.95986357965387),
                       /*47*/new TestSystem.TestDatum( "a b ab ac bc abc", 341.95986357965387),
                       /*48*/new TestSystem.TestDatum( "a b ab c ac bc abc d ad bd abd cd acd bcd abcd", 480.2882471117551),
                       /*49*/new TestSystem.TestDatum( "a b ab c ac bc abc cd acd bcd abcd cde acde bcde abcde", 501.609114069913),
                       /*49*/new TestSystem.TestDatum( "a b ab c ac bc abc cd acd bcd abcd cde acde bcde abcde", 501.609114069913),
                       /*50*/new TestSystem.TestDatum( "a b ab c ac bc abc d ad bd abd cd acd bcd abcd cde acde bcde abcde", 633.810500520683),
                       /*50*/new TestSystem.TestDatum( "a b ab c ac bc abc d ad bd abd cd acd bcd abcd cde acde bcde abcde", 633.810500520683),
                       /*51*/new TestSystem.TestDatum( "abcd abce", 508.7862565358031),
                       /*51*/new TestSystem.TestDatum( "abcd abce", 508.7862565358031),
                       /*52*/new TestSystem.TestDatum( "a ab c cd", 307.1661520191668),
                       /*52*/new TestSystem.TestDatum( "a ab c cd", 307.1661520191668),
                       /*53*/new TestSystem.TestDatum( "a c ab bc", 225.14440110878547),
                       /*54*/new TestSystem.TestDatum( "a b ac bc bcd d", 458.5317063123452),
                       /*54*/new TestSystem.TestDatum( "a b ac bc bcd d", 458.5317063123452),
                       /*55*/new TestSystem.TestDatum( "abcd abce de", 996.2883327546962),
                       /*55*/new TestSystem.TestDatum( "abcd abce de", 996.2883327546962),
                       /*56*/new TestSystem.TestDatum( "a b ab c ac bc abc df adf bdf abdf cd acd bcd abcd cde acde bcde abcde", 841.2281428103137),
                       /*56*/new TestSystem.TestDatum( "a b ab c ac bc abc df adf bdf abdf cd acd bcd abcd cde acde bcde abcde", 841.2281428103137),
                       /*57*/new TestSystem.TestDatum( "abd abc dc", 610.4957135939354),
                       /*57*/new TestSystem.TestDatum( "abd abc dc", 610.4957135939354),
                       /*58*/new TestSystem.TestDatum( "a b ab c ac bc abc p q pq r pr qr pqr x bx px", 749.3046043410096),
                       /*58*/new TestSystem.TestDatum( "a b ab c ac bc abc p q pq r pr qr pqr x bx px", 749.3046043410096),
                       /*59*/new TestSystem.TestDatum( "a b ab c ac d ad e ae f af", 623.4193360283326),
                       /*60*/new TestSystem.TestDatum( "a b c d cd ae be e ce de cde", 473.4313453028586),
                       /*61*/new TestSystem.TestDatum( "a b c d cd ae be e ce de cde ef", 624.755861723394),
                       /*62*/new TestSystem.TestDatum( "a b c ab ac bc abc ad", 353.72951221911853),
                       /*62*/new TestSystem.TestDatum( "a b c ab ac bc abc ad", 353.72951221911853),
                       /*63*/new TestSystem.TestDatum( "a b c ab ac bc abc abd", 340.82499327267107),
                       /*63*/new TestSystem.TestDatum( "a b c ab ac bc abc abd", 340.82499327267107),
                       /*64*/new TestSystem.TestDatum( "a b c ab ac bc abc abcd", 339.83033139030465),
                       /*64*/new TestSystem.TestDatum( "a b c ab ac bc abc abcd", 339.83033139030465),
                       /*65*/new TestSystem.TestDatum( "ad bd cd abd acd bcd abcd d", 389.29950581143595),
                       /*65*/new TestSystem.TestDatum( "ad bd cd abd acd bcd abcd d", 389.29950581143595),
                       /*66*/new TestSystem.TestDatum( "a b c ab ac bc abc ad bd cd", 579.5832494842473),
                       /*66*/new TestSystem.TestDatum( "a b c ab ac bc abc ad bd cd", 579.5832494842473),
                       /*67*/new TestSystem.TestDatum( "a b c ab ac bc abc abd bcd acd", 588.374389209913),
                       /*67*/new TestSystem.TestDatum( "a b c ab ac bc abc abd bcd acd", 588.374389209913),
                       /*68*/new TestSystem.TestDatum( "a b c ab ac bc abc ad d", 329.37716678546525),
                       /*69*/new TestSystem.TestDatum( "a b c ab ac bc abc ad abd", 346.01510503713456),
                       /*69*/new TestSystem.TestDatum( "a b c ab ac bc abc ad abd", 346.01510503713456),
                       /*70*/new TestSystem.TestDatum( "a b c ab ac bc abc abd abcd", 340.75455505723016),
                       /*70*/new TestSystem.TestDatum( "a b c ab ac bc abc abd abcd", 340.75455505723016),
                       /*71*/new TestSystem.TestDatum( "a b c ab ac bc abc ad d be e cf f", 554.2867469715592),
                       /*72*/new TestSystem.TestDatum( "a b c ab ac bc abc ad bd abd d", 355.41370661062354),
                       /*73*/new TestSystem.TestDatum( "a b c ab ac bc abc acd bcd abcd cd", 365.61327622737673),
                       /*74*/new TestSystem.TestDatum( "a ab b ac c ad d be e cf f dg g", 739.1650874845034),
                       /*75*/new TestSystem.TestDatum( "a ab b ac c ad d be e cf f dg g eh h fi i gj j ak k kl l lm m", 2755.650253481586),
                       /*76*/new TestSystem.TestDatum( "ab ac abc ad ae ade", 487.10424505218145),
                       /*76*/new TestSystem.TestDatum( "ab ac abc ad ae ade", 487.10424505218145),
                       /*77*/new TestSystem.TestDatum( "a b ab c ac abd ace", 469.1005971050783),
                       /*77*/new TestSystem.TestDatum( "a b ab c ac abd ace", 469.1005971050783),
                       /*78*/new TestSystem.TestDatum( "a b ab c ac d ad be ce de", 913.593066743922),
                       /*78*/new TestSystem.TestDatum( "a b ab c ac d ad be ce de", 913.593066743922),
                       /*79*/new TestSystem.TestDatum( "a b ab c ac d ad ae be ce de", 955.963468227657),
                       /*79*/new TestSystem.TestDatum( "a b ab c ac d ad ae be ce de", 955.963468227657),
                       /*80*/new TestSystem.TestDatum( "a b ab c ac abd ace acef acefg", 790.8953195743954),
                       /*80*/new TestSystem.TestDatum( "a b ab c ac abd ace acef acefg", 790.8953195743954),
                       /*81*/new TestSystem.TestDatum( "qh h fh ih ik kh b ab ac de bd  abc bfg", 2881.4574729004075),
                       /*81*/new TestSystem.TestDatum( "qh h fh ih ik kh b ab ac de bd  abc bfg", 2881.4574729004075),
                       /*82*/new TestSystem.TestDatum( "qh h fh ih ik kh b ab ac de bd  abc bfg fc", 3168.2249837673676),
                       /*82*/new TestSystem.TestDatum( "qh h fh ih ik kh b ab ac de bd  abc bfg fc", 3168.2249837673676),
                       /*83*/new TestSystem.TestDatum( "qh h fh ih ik kh b ab ac de bd  abc bfg fc bj", 3771.9321696318084),
                       /*83*/new TestSystem.TestDatum( "qh h fh ih ik kh b ab ac de bd  abc bfg fc bj", 3771.9321696318084),
                       /*84*/new TestSystem.TestDatum( "qh h fh ih ik kh b ab ac de bd  abc bfg fc bj l", 4327.697005220982),
                       /*84*/new TestSystem.TestDatum( "qh h fh ih ik kh b ab ac de bd  abc bfg fc bj l", 4327.697005220982),
                       /*85*/new TestSystem.TestDatum( "qh h fh ih ik kh b ab ac de bd  abc bfg fc bj l lc", 4239.880685561009),
                       /*85*/new TestSystem.TestDatum( "qh h fh ih ik kh b ab ac de bd  abc bfg fc bj l lc", 4239.880685561009),
                       /*86*/new TestSystem.TestDatum( "qh h fh ih ik kh b ab ac de bd  abc bfg fc bj l lc al", 5991.800650112288),
                       /*86*/new TestSystem.TestDatum( "qh h fh ih ik kh b ab ac de bd  abc bfg fc bj l lc al", 5991.800650112288),
                       /*87*/new TestSystem.TestDatum( "qh h fh ih ik kh b ab ac de bd  abc bfg fc bj l lc al m mn nc bc bco bo boj bp bop cq cqb rs ra s", 0.0),
                       /*87*/new TestSystem.TestDatum( "qh h fh ih ik kh b ab ac de bd  abc bfg fc bj l lc al m mn nc bc bco bo boj bp bop cq cqb rs ra s", 0.0),
                       /*88*/new TestSystem.TestDatum( ",", 0.0),
                       /*89*/new TestSystem.TestDatum( ",.", 0.0),
                       /*90*/new TestSystem.TestDatum( "a,", 80.35747263647977),
                       /*91*/new TestSystem.TestDatum( "a,.", 80.35747263647977),
                       /*92*/new TestSystem.TestDatum( "a,a", 80.38754263647976),
                       /*93*/new TestSystem.TestDatum( "a,. a", 80.38754263647976),
                       /*94*/new TestSystem.TestDatum( "a b ab,", 151.7818962901353),
                       /*95*/new TestSystem.TestDatum( "a b ab, a", 151.8119662901353),
                       /*96*/new TestSystem.TestDatum( "a b ab, b", 151.81227629013532),
                       /*97*/new TestSystem.TestDatum( "a b ab, ab", 151.8432282901353),
                       /*98*/new TestSystem.TestDatum( "a b ab, .", 151.7818962901353),
                       /*99*/new TestSystem.TestDatum( "a b ab, . a", 151.8119662901353),
                       /*100*/new TestSystem.TestDatum( "a b ab, . b", 151.81227629013532),
                       /*101*/new TestSystem.TestDatum( "a b ab, . a b", 151.8423462901353),
                       /*102*/new TestSystem.TestDatum( "a b ab, . a b ab", 151.9036782901353),
                       /*103*/new TestSystem.TestDatum( "a ab c abc, ", 248.06646143598684),
                       /*104*/new TestSystem.TestDatum( "a ab c abc,.", 248.06646143598684),
                       /*105*/new TestSystem.TestDatum( "a ab c abc,a", 248.09653143598683),
                       /*106*/new TestSystem.TestDatum( "a ab c abc,ab", 248.12779343598683),
                       /*107*/new TestSystem.TestDatum( "a ab c abc,a ab", 248.15786343598683),
                       /*108*/new TestSystem.TestDatum( "a b ab, ,a 'my_label", 310.9439177650555),
                       /*109*/new TestSystem.TestDatum( "a b ab, ,b 'label2", 234.9945557984952),
                       /*110*/new TestSystem.TestDatum( "a b ab, ,a 'sa, b 'sb", 425.9889815683995),
                       /*111*/new TestSystem.TestDatum( "a b ab, ,., a", 509.99511776505557),
                       /*112*/new TestSystem.TestDatum( "a b ab, ,ab", 272.96923678177535),
                       /*113*/new TestSystem.TestDatum( "a b ab, ,a, ab", 463.9636625516796),
                       /*114*/new TestSystem.TestDatum( "a b ab, ,b, ab", 372.8244281918072),
                       /*115*/new TestSystem.TestDatum( "a b ab, ,a, ab, .", 692.2760158039885),
                       /*116*/new TestSystem.TestDatum( "a b ab, ,a, ab, ., b", 883.5874992151189),
                       /*117*/new TestSystem.TestDatum( "a b ab, ,a b", 743.9813858633836),
                       /*118*/new TestSystem.TestDatum( "a b ab, ,a b, . ab", 1586.4416393021174),
                       /*118*/new TestSystem.TestDatum( "a b ab, ,a b, . ab", 1586.4416393021174),
                       /*119*/new TestSystem.TestDatum( "a b c ab ac bc abc ad, ,a b c abc ac", 0.0),
                       /*119*/new TestSystem.TestDatum( "a b c ab ac bc abc ad, ,a b c abc ac", 0.0),
                       /*120*/new TestSystem.TestDatum( "a b c ab ac bc abc ad, ,a b c abc", 0.0),
                       /*120*/new TestSystem.TestDatum( "a b c ab ac bc abc ad, ,a b c abc", 0.0),
                       /*121*/new TestSystem.TestDatum( "A B AB, ,A B, AB", 983.6086195810333),
                       /*121*/new TestSystem.TestDatum( "A B AB, ,A B, AB", 983.6086195810333),
                       /*122*/new TestSystem.TestDatum( "A B AB, ,A, AB", 463.9636578630396),
                       /*123*/new TestSystem.TestDatum( "A B AB, ,B, AB", 372.8244235031673),
                       /*124*/new TestSystem.TestDatum( "A B AB, ,A AB,B AB", 0.0),
                       /*124*/new TestSystem.TestDatum( "A B AB, ,A AB,B AB", 0.0),
                       /*125*/new TestSystem.TestDatum( "A B AB, ,A AB,B", 1086.8997518555555),
                       /*126*/new TestSystem.TestDatum( "A B AB, ,B AB,A", 971.4567216663836),
                       /*127*/new TestSystem.TestDatum( "A B C AB AC BC ABC, B,A AB ABC, B, B", 0.0),
                       /*127*/new TestSystem.TestDatum( "A B C AB AC BC ABC, B,A AB ABC, B, B", 0.0),
                       /*128*/new TestSystem.TestDatum( ",", 0.0),
                       /*129*/new TestSystem.TestDatum( ",,", 0.0),
                       /*130*/new TestSystem.TestDatum( "a,,a .", 845.5434726364797),
                       /*131*/new TestSystem.TestDatum( "a,.,a .,.", 1055.5206726364797),
                       /*132*/new TestSystem.TestDatum( "a,.,a .,.,.,.", 2033.7907206364794),
                       /*133*/new TestSystem.TestDatum( "A B AB, ,A B AB .,B AB", 0.0),
                       /*133*/new TestSystem.TestDatum( "A B AB, ,A B AB .,B AB", 0.0)
        };

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testSystem() {
        for(TestSystem.TestDatum datum : testData) {
            AbstractDescription ad = null;
            try {
                ad = mapper.readValue(datum.getDescription(), AbstractDescription.class);
            } catch (Exception e ) {
            	e.printStackTrace();
                // This should not happen
                assertTrue(false);
            }

            DiagramCreator      dc = new DiagramCreator(ad);
            try {
                // arbitrary canvas size
                ConcreteDiagram cd = dc.createDiagram(200);
                try {
                // Eplsilon value from Jean's original TestCode.java
                assertEquals(datum.getChecksum(), cd.checksum(), 0.001);
                } catch (AssertionError ae) {
                    // This is a hack to save the current diagram if
                    // assertEquals fails
                    PrintStream out = null;
                    try {
                        String tmpFileName = UUID.randomUUID().toString() + ".svg";
                        out = new PrintStream(new FileOutputStream(tmpFileName));
                        CirclesSVGGenerator csg = new CirclesSVGGenerator(cd);
                        out.print(csg.toString());
                    } catch (FileNotFoundException fnfe) {
                        // do nothing
                    }
                    finally {
                        if (out != null) out.close();
                    }

                    // rethrow the AssertionError
                    throw ae;
                }
            } catch (CannotDrawException cde) {
            	// Diagrams throwing CannotDrawException have an expected
                // checksum of 0.0
                assertEquals(0.0, datum.getChecksum(), 0.0);
            }
        }
    }
}
