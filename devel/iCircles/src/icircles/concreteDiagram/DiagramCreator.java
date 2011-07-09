package icircles.concreteDiagram;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import icircles.decomposition.DecompositionStep;

import icircles.abstractDescription.AbstractBasicRegion;
import icircles.abstractDescription.AbstractCurve;
import icircles.abstractDescription.AbstractDescription;
import icircles.abstractDescription.CurveLabel;
import icircles.recomposition.RecompData;
import icircles.recomposition.RecompositionStep;
import icircles.util.CannotDrawException;
import icircles.util.DEB;

public class DiagramCreator {

    final static int smallest_rad = 3;
    ArrayList<DecompositionStep> d_steps;
    ArrayList<RecompositionStep> r_steps;
    HashMap<AbstractBasicRegion, Double> zoneScores;
    HashMap<AbstractCurve, Double> contScores;
    HashMap<AbstractCurve, Double> guide_sizes;
    HashMap<AbstractCurve, CircleContour> map;
    ArrayList<CircleContour> circles;

    public DiagramCreator(ArrayList<DecompositionStep> d_steps,
            ArrayList<RecompositionStep> r_steps,
            int size) {
        this.d_steps = d_steps;
        this.r_steps = r_steps;
        map = new HashMap<AbstractCurve, CircleContour>();
    }

    public ConcreteDiagram createDiagram(int size) throws CannotDrawException {
        make_guide_sizes(); // scores zones too
        Rectangle2D.Double box = null;
        if (guide_sizes.size() < 1) {
            box = new Rectangle2D.Double(0, 0, 1000, 1000);
        } else {
            box = new Rectangle2D.Double(0, 0, 1000 * guide_sizes.size(), 1000 * guide_sizes.size());
        }
        circles = new ArrayList<CircleContour>();
        boolean ok = createCircles(box);
        
        // Some temp code to add seven spider feet in each zone
        /* 
        RecompositionStep last_step = r_steps.get(r_steps.size() - 1);
        AbstractDescription last_diag = last_step.to();
        Iterator<AbstractBasicRegion> it = last_diag.getZoneIterator();
                
        while(it.hasNext())
	        {
	        AbstractBasicRegion abr  = it.next();
	        
	        ArrayList<CurveLabel> labels = new ArrayList<CurveLabel>();
	        labels.add(null);
	        labels.add(null);
	        labels.add(null);
	        labels.add(null);
	        labels.add(null);
	        labels.add(null);
	        labels.add(null);

            ArrayList<CircleContour> cs = findCircleContours(box, smallest_rad, 3,
                    abr, last_diag, labels);

            for(CircleContour cc : cs)
	            {
	    		cc.radius = 1;
	    		circles.add(cc);
		        }
	        }
		*/
		
        if (!ok) {
            circles = null;
            return null;
        }

        // work out a suitable size
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (CircleContour cc : circles) {
            if (cc.getMinX() < minX) {
                minX = cc.getMinX();
            }
            if (cc.getMinY() < minY) {
                minY = cc.getMinY();
            }
            if (cc.getMaxX() > maxX) {
                maxX = cc.getMaxX();
            }
            if (cc.getMaxY() > maxY) {
                maxY = cc.getMaxY();
            }
        }

        double midX = (minX + maxX) * 0.5;
        double midY = (minY + maxY) * 0.5;
        for (CircleContour cc : circles) {
            cc.shift(-midX, -midY);
        }

        double width = maxX - minX;
        double height = maxY - minY;
        double biggest_HW = Math.max(height, width);
        double scale = (size * 0.95) / biggest_HW;
        for (CircleContour cc : circles) {
            cc.scaleAboutZero(scale);
        }

        for (CircleContour cc : circles) {
            cc.shift(size * 0.5, size * 0.5);
        }

        ArrayList<ConcreteZone> shadedZones = createShadedZones();
        ConcreteDiagram result = new ConcreteDiagram(new Rectangle2D.Double(0, 0, size, size),
                circles, shadedZones);
        return result;
    }

    private void make_guide_sizes() {
        guide_sizes = new HashMap<AbstractCurve, Double>();
        if (r_steps.size() == 0) {
            return;
        }

        RecompositionStep last_step = r_steps.get(r_steps.size() - 1);
        AbstractDescription last_diag = last_step.to();

        zoneScores = new HashMap<AbstractBasicRegion, Double>();
        double total_score = 0.0;
        {
            Iterator<AbstractBasicRegion> zIt = last_diag.getZoneIterator();
            while (zIt.hasNext()) {
                AbstractBasicRegion abr = zIt.next();
                double score = scoreZone(abr, last_diag);
                total_score += score;
                zoneScores.put(abr, score);
            }
        }

        contScores = new HashMap<AbstractCurve, Double>();
        Iterator<AbstractCurve> cIt = last_diag.getContourIterator();
        while (cIt.hasNext()) {
            AbstractCurve ac = cIt.next();
            double cScore = 0;
            Iterator<AbstractBasicRegion> zIt = last_diag.getZoneIterator();
            int numZones = 0;
            while (zIt.hasNext()) {
                AbstractBasicRegion abr = zIt.next();
                if (abr.is_in(ac)) {
                    cScore += zoneScores.get(abr);
                }
                numZones++;
            }
            contScores.put(ac, cScore);
            double guide_size = Math.exp(0.75 * Math.log(cScore / total_score)) * 100;
            guide_sizes.put(ac, guide_size);
        }
    }

    private double scoreZone(AbstractBasicRegion abr, AbstractDescription context) {
        return 1.0;
    }

    private ArrayList<ConcreteZone> createShadedZones() {
        ArrayList<ConcreteZone> result = new ArrayList<ConcreteZone>();
        if (d_steps.size() == 0) {
            return result;
        }
        AbstractDescription initial_diagram = d_steps.get(0).from();
        AbstractDescription final_diagram = r_steps.get(r_steps.size() - 1).to();
        // which zones in final_diagram were not in initial_diagram?

        if (DEB.level > 2) {
            Iterator<AbstractBasicRegion> it = initial_diagram.getZoneIterator();
            while (it.hasNext()) {
                System.out.println("initial zone " + it.next().debug());
            }
            it = final_diagram.getZoneIterator();
            while (it.hasNext()) {
                System.out.println("final zone " + it.next().debug());
            }
        }

        Iterator<AbstractBasicRegion> it = final_diagram.getZoneIterator();
        while (it.hasNext()) {
            AbstractBasicRegion z = it.next();
            if (!initial_diagram.hasLabelEquivalentZone(z)) {
                // we have an extra zone
                if (DEB.level > 2) {
                    System.out.println("extra zone " + z.debug());
                }
                ConcreteZone cz = makeConcreteZone(z);
                result.add(cz);
            }
        }
        return result;
    }

    private ConcreteZone makeConcreteZone(AbstractBasicRegion z) {
        ArrayList<CircleContour> includingCircles = new ArrayList<CircleContour>();
        ArrayList<CircleContour> excludingCircles = new ArrayList<CircleContour>(circles);
        Iterator<AbstractCurve> acIt = z.getContourIterator();
        while (acIt.hasNext()) {
            AbstractCurve ac = acIt.next();
            CircleContour containingCC = map.get(ac);
            excludingCircles.remove(containingCC);
            includingCircles.add(containingCC);
        }
        ConcreteZone cz = new ConcreteZone(z, includingCircles, excludingCircles);
        return cz;
    }

    private boolean createCircles(Rectangle2D.Double box) throws CannotDrawException {
        BuildStep bs = null;
        BuildStep tail = null;
        for (RecompositionStep rs : r_steps) {
            // we need to add the new curves with regard to their placement
            // relative to the existing ones in the map
            Iterator<RecompData> it = rs.getRecompIterator();
            while (it.hasNext()) {
                RecompData rd = it.next();
                BuildStep newOne = new BuildStep(rd);
                if (bs == null) {
                    bs = newOne;
                    tail = newOne;
                } else {
                    tail.next = newOne;
                    tail = newOne;
                }
            }
        }

        shuffle_and_combine(bs);

        BuildStep step = bs;
        stepLoop:
        while (step != null) {
            DEB.out(2, "new build step");
            // we need to add the new curves with regard to their placement
            // relative to the existing ones in the map
            if (step.recomp_data.size() > 1) {
                if (step.recomp_data.get(0).split_zones.size() == 1) {
                    // we have a symmetry of nested contours.
                    // try to add them together
                    RecompData rd = step.recomp_data.get(0);
                    AbstractBasicRegion zone = rd.split_zones.get(0);

                    RecompositionStep last_step = r_steps.get(r_steps.size() - 1);
                    AbstractDescription last_diag = last_step.to();

                    AbstractCurve ac = rd.added_curve;
                    double suggested_rad = guide_sizes.get(ac);

                    ArrayList<CurveLabel> labels = new ArrayList<CurveLabel>();
                    for (RecompData rd2 : step.recomp_data) {
                        ac = rd2.added_curve;
                        labels.add(ac.getLabel());
                    }

                    ArrayList<CircleContour> cs = findCircleContours(box, smallest_rad, suggested_rad,
                            zone, last_diag, labels);
                    if (cs != null && cs.size() > 0) {
                        DEB.assertCondition(cs.size() == step.recomp_data.size(), "not enough circles for rds");
                        for (int i = 0; i < cs.size(); i++) {
                            CircleContour c = cs.get(i);
                            ac = step.recomp_data.get(i).added_curve;
                            DEB.assertCondition(
                                    c.l == ac.getLabel(), "mismatched labels");
                            map.put(ac, c);
                            addCircle(c);
                        }
                        step = step.next;
                        continue stepLoop;
                    }
                } else if (step.recomp_data.get(0).split_zones.size() == 2) {
                    // we have a symmetry of 1-piercings.
                    // try to add them together
                    AbstractBasicRegion abr0 = step.recomp_data.get(0).split_zones.get(0);
                    AbstractBasicRegion abr1 = step.recomp_data.get(0).split_zones.get(1);
                    AbstractCurve ac = abr0.getStraddledContour(abr1);
                    ConcreteZone cz0 = makeConcreteZone(abr0);
                    ConcreteZone cz1 = makeConcreteZone(abr1);
                    Area a = new Area(cz0.getShape(box));
                    a.add(cz1.getShape(box));
                    CircleContour pierced_curve = map.get(ac);
                    double guide_rad = guide_sizes.get(step.recomp_data.get(0).added_curve);
                    int sampleSize = (int) (Math.PI / Math.asin(guide_rad / pierced_curve.radius));
                    if (sampleSize >= step.recomp_data.size()) {
                        int num_ok = 0;
                        for (int i = 0; i < sampleSize; i++) {
                            double angle = i * Math.PI * 2.0 / sampleSize;
                            double x = pierced_curve.cx + Math.cos(angle) * pierced_curve.radius;
                            double y = pierced_curve.cy + Math.sin(angle) * pierced_curve.radius;
                            if (a.contains(x, y)) {
                                CircleContour sample = new CircleContour(x, y, guide_rad,
                                        step.recomp_data.get(0).added_curve.getLabel());
                                if (containedIn(sample, a)) {
                                    num_ok++;
                                }
                            }
                        }
                        if (num_ok >= step.recomp_data.size()) {
                            if (num_ok == sampleSize) {
                                // all OK.
                                for (int i = 0; i < step.recomp_data.size(); i++) {
                                    double angle = 0.0 + i * Math.PI * 2.0 / step.recomp_data.size();
                                    double x = pierced_curve.cx + Math.cos(angle) * pierced_curve.radius;
                                    double y = pierced_curve.cy + Math.sin(angle) * pierced_curve.radius;
                                    if (a.contains(x, y)) {
                                        AbstractCurve added_curve = step.recomp_data.get(i).added_curve;
                                        CircleContour c = new CircleContour(x, y, guide_rad, added_curve.getLabel());
                                        abr0 = step.recomp_data.get(i).split_zones.get(0);
                                        abr1 = step.recomp_data.get(i).split_zones.get(1);
                                        ac = abr0.getStraddledContour(abr1);
                                        map.put(added_curve, c);
                                        addCircle(c);
                                    }
                                }
                                step = step.next;
                                continue stepLoop;
                            } else if (num_ok > sampleSize) {
                                num_ok = 0;
                                for (int i = 0; i < sampleSize; i++) {
                                    double angle = 0.0 + i * Math.PI * 2.0 / sampleSize;
                                    double x = pierced_curve.cx + Math.cos(angle) * pierced_curve.radius;
                                    double y = pierced_curve.cy + Math.sin(angle) * pierced_curve.radius;
                                    if (a.contains(x, y)) {
                                        AbstractCurve added_curve = step.recomp_data.get(i).added_curve;
                                        CircleContour c = new CircleContour(x, y, guide_rad, added_curve.getLabel());
                                        if (containedIn(c, a)) {
                                            abr0 = step.recomp_data.get(num_ok).split_zones.get(0);
                                            abr1 = step.recomp_data.get(num_ok).split_zones.get(1);
                                            map.put(added_curve, c);
                                            addCircle(c);
                                            num_ok++;
                                            if (num_ok == step.recomp_data.size()) {
                                                break;
                                            }
                                        }
                                    }
                                }
                                step = step.next;
                                continue stepLoop;
                            }
                        }
                    }
                }
            }
            for (RecompData rd : step.recomp_data) {
                AbstractCurve ac = rd.added_curve;
                double suggested_rad = guide_sizes.get(ac);
                if (rd.split_zones.size() == 1) {
                    // add a nested contour---------------------------------------------------
                    // add a nested contour---------------------------------------------------
                    // add a nested contour---------------------------------------------------

                    // look ahead - are we going to add a piercing to this?
                    // if so, push it to one side to make space
                    boolean will_pierce = false;
                    BuildStep future_bs = bs.next;
                    while (future_bs != null) {
                        if (future_bs.recomp_data.get(0).split_zones.size() == 2) {
                            AbstractBasicRegion abr0 = future_bs.recomp_data.get(0).split_zones.get(0);
                            AbstractBasicRegion abr1 = future_bs.recomp_data.get(0).split_zones.get(1);
                            AbstractCurve ac_future = abr0.getStraddledContour(abr1);
                            if (ac_future == ac) {
                                will_pierce = true;
                                break;
                            }
                        }
                        future_bs = future_bs.next;
                    }

                    if (DEB.level > 3) {
                        System.out.println("make a nested contour");
                    }
                    // make a circle inside containingCircles, outside excludingCirles.

                    AbstractBasicRegion zone = rd.split_zones.get(0);

                    RecompositionStep last_step = r_steps.get(r_steps.size() - 1);
                    AbstractDescription last_diag = last_step.to();

                    CircleContour c = findCircleContour(box, smallest_rad, suggested_rad,
                            zone, last_diag, ac.getLabel());
                    if (c == null) {
                        throw new CannotDrawException("cannot place nested contour");
                    }

                    if (will_pierce && rd.split_zones.get(0).getNumContours() > 0) {
                        // nudge to the left
                        c.cx -= c.radius * 0.5;

                        ConcreteZone cz = makeConcreteZone(rd.split_zones.get(0));
                        Area a = new Area(cz.getShape(box));
                        if (!containedIn(c, a)) {
                            c.cx += c.radius * 0.25;
                            c.radius *= 0.75;
                        }
                    }
                    map.put(ac, c);
                    addCircle(c);
                } else if (rd.split_zones.size() == 2) {
                    // add a single piercing---------------------------------------------------
                    // add a single piercing---------------------------------------------------
                    // add a single piercing---------------------------------------------------

                    if (DEB.level > 3) {
                        System.out.println("make a single-piercing contour");
                    }
                    AbstractBasicRegion abr0 = rd.split_zones.get(0);
                    AbstractBasicRegion abr1 = rd.split_zones.get(1);
                    AbstractCurve c = abr0.getStraddledContour(abr1);
                    CircleContour cc = map.get(c);
                    ConcreteZone cz0 = makeConcreteZone(abr0);
                    ConcreteZone cz1 = makeConcreteZone(abr1);
                    Area a = new Area(cz0.getShape(box));
                    a.add(cz1.getShape(box));
                    // now place circles around cc, checking whether they fit into a
                    CircleContour solution = null;
                    for (int angleI = 0; angleI < 20; angleI++) {
                        double angle = Math.PI * 2 * 20.0 / angleI;
                        double x = cc.cx + Math.cos(angle) * cc.radius;
                        double y = cc.cy + Math.sin(angle) * cc.radius;
                        if (a.contains(x, y)) {
                            // how big a circle can we make?
                            double start_rad;
                            if (solution != null) {
                                start_rad = solution.radius + smallest_rad;
                            } else {
                                start_rad = smallest_rad;
                            }
                            CircleContour attempt = growCircleContour(a, rd.added_curve.getLabel(), x, y, suggested_rad, start_rad, smallest_rad);
                            if (attempt != null) {
                                solution = attempt;
                                if (solution.radius == guide_sizes.get(ac)) {
                                    break; // no need to try any more
                                }
                            }

                        }//check that the centre is ok
                    }// loop for different centre placement
                    if (solution == null) // no single piercing found which was OK
                    {
                        throw new CannotDrawException("1-peircing no fit");
                    } else {
                        DEB.out(2, "added a single piercing labelled " + solution.l.getLabel());
                        map.put(rd.added_curve, solution);
                        addCircle(solution);
                    }
                } else {
                    //double piercing
                    AbstractBasicRegion abr0 = rd.split_zones.get(0);
                    AbstractBasicRegion abr1 = rd.split_zones.get(1);
                    AbstractBasicRegion abr2 = rd.split_zones.get(2);
                    AbstractBasicRegion abr3 = rd.split_zones.get(3);
                    AbstractCurve c1 = abr0.getStraddledContour(abr1);
                    AbstractCurve c2 = abr0.getStraddledContour(abr2);
                    CircleContour cc1 = map.get(c1);
                    CircleContour cc2 = map.get(c2);

                    double[][] intn_coords = intersect(cc1.cx, cc1.cy, cc1.radius,
                            cc2.cx, cc2.cy, cc2.radius);
                    if (intn_coords == null) {
                        System.out.println("double piercing on non-intersecting circles");
                        return false;
                    }

                    ConcreteZone cz0 = makeConcreteZone(abr0);
                    ConcreteZone cz1 = makeConcreteZone(abr1);
                    ConcreteZone cz2 = makeConcreteZone(abr2);
                    ConcreteZone cz3 = makeConcreteZone(abr3);
                    Area a = new Area(cz0.getShape(box));
                    a.add(cz1.getShape(box));
                    a.add(cz2.getShape(box));
                    a.add(cz3.getShape(box));
                    double cx, cy;
                    if (a.contains(intn_coords[0][0], intn_coords[0][1])) {
                        if (DEB.level > 2) {
                            System.out.println("intn at (" + intn_coords[0][0] + "," + intn_coords[0][1] + ")");
                        }
                        cx = intn_coords[0][0];
                        cy = intn_coords[0][1];
                    } else if (a.contains(intn_coords[1][0], intn_coords[1][1])) {
                        if (DEB.level > 2) {
                            System.out.println("intn at (" + intn_coords[1][0] + "," + intn_coords[1][1] + ")");
                        }
                        cx = intn_coords[1][0];
                        cy = intn_coords[1][1];
                    } else {
                        if (DEB.level > 2) {
                            System.out.println("no suitable intn for double piercing");
                        }
                        throw new CannotDrawException("2peircing + disjoint");
                    }

                    CircleContour solution = growCircleContour(a, rd.added_curve.getLabel(), cx, cy,
                            suggested_rad, smallest_rad, smallest_rad);
                    if (solution == null) // no double piercing found which was OK
                    {
                        throw new CannotDrawException("2peircing no fit");
                    } else {
                        DEB.out(2, "added a double piercing labelled " + solution.l.getLabel());
                        map.put(rd.added_curve, solution);
                        addCircle(solution);
                    }
                }// if/else/else about piercing type
            }// next RecompData in the BuildStep
            step = step.next;
        }// go to next BuildStep
        return true;
    }

    private void shuffle_and_combine(BuildStep steplist) {
        // collect together additions which are
        //  (i) nested in the same zone
        //  (ii) single-piercings with the same zones
        //  (iii) will have the same radius (have the same "score")

        BuildStep bs = steplist;
        while (bs != null) {
            DEB.assertCondition(bs.recomp_data.size() == 1, "not ready for multistep");
            if (bs.recomp_data.get(0).split_zones.size() == 1) {
                RecompData rd = bs.recomp_data.get(0);
                AbstractBasicRegion abr = rd.split_zones.get(0);
                // look ahead - are there other similar nested additions?
                BuildStep beforefuturebs = bs;
                while (beforefuturebs != null && beforefuturebs.next != null) {
                    RecompData rd2 = beforefuturebs.next.recomp_data.get(0);
                    if (rd2.split_zones.size() == 1) {
                        AbstractBasicRegion abr2 = rd2.split_zones.get(0);
                        if (abr.isLabelEquivalent(abr2)) {
                            DEB.out(2, "found matching abrs " + abr.debug() + ", " + abr2.debug());
                            // check scores match

                            double abrScore = contScores.get(rd.added_curve);
                            double abrScore2 = contScores.get(rd2.added_curve);
                            DEB.assertCondition(abrScore > 0 && abrScore2 > 0, "zones must have score");
                            DEB.out(2, "matched nestings " + abr.debug() + " and " + abr2.debug()
                                    + "\n with scores " + abrScore + " and " + abrScore2);
                            if (abrScore == abrScore2) {
                                // unhook futurebs and insert into list after bs
                                BuildStep to_move = beforefuturebs.next;
                                beforefuturebs.next = to_move.next;

                                bs.recomp_data.add(to_move.recomp_data.get(0));
                            }
                        }
                    }
                    beforefuturebs = beforefuturebs.next;
                }// loop through futurebs's to see if we insert another
            }// check - are we adding a nested contour?
            else if (bs.recomp_data.get(0).split_zones.size() == 2) {// we are adding a 1-piercing
                RecompData rd = bs.recomp_data.get(0);
                AbstractBasicRegion abr1 = rd.split_zones.get(0);
                AbstractBasicRegion abr2 = rd.split_zones.get(1);
                // look ahead - are there other similar 1-piercings?
                BuildStep beforefuturebs = bs;
                while (beforefuturebs != null && beforefuturebs.next != null) {
                    RecompData rd2 = beforefuturebs.next.recomp_data.get(0);
                    if (rd2.split_zones.size() == 2) {
                        AbstractBasicRegion abr3 = rd2.split_zones.get(0);
                        AbstractBasicRegion abr4 = rd2.split_zones.get(1);
                        if ((abr1.isLabelEquivalent(abr3) && abr2.isLabelEquivalent(abr4))
                                || (abr1.isLabelEquivalent(abr4) && abr2.isLabelEquivalent(abr3))) {

                            DEB.out(2, "found matching abrs " + abr1.debug() + ", " + abr2.debug());
                            // check scores match
                            double abrScore = contScores.get(rd.added_curve);
                            double abrScore2 = contScores.get(rd2.added_curve);
                            DEB.assertCondition(abrScore > 0 && abrScore2 > 0, "zones must have score");
                            DEB.out(2, "matched piercings " + abr1.debug() + " and " + abr2.debug()
                                    + "\n with scores " + abrScore + " and " + abrScore2);
                            if (abrScore == abrScore2) {
                                // unhook futurebs and insert into list after bs
                                BuildStep to_move = beforefuturebs.next;
                                beforefuturebs.next = to_move.next;

                                bs.recomp_data.add(to_move.recomp_data.get(0));
                                continue;
                            }
                        }
                    }
                    beforefuturebs = beforefuturebs.next;
                }// loop through futurebs's to see if we insert another
            }

            bs = bs.next;
        }// bsloop
    }

    void addCircle(CircleContour c) {
        if (DEB.level > 2) {
            System.out.println("adding " + c.debug());
        }
        circles.add(c);
    }

    private CircleContour growCircleContour(Area a, CurveLabel l,
            double cx, double cy,
            double suggested_rad, double start_rad,
            double smallest_rad) {
        CircleContour attempt = new CircleContour(cx, cy, suggested_rad, l);
        if (containedIn(attempt, a)) {
            return new CircleContour(cx, cy, suggested_rad, l);
        }

        boolean ok = true;
        double good_rad = -1.0;
        double rad = start_rad;
        while (ok) {
            attempt = new CircleContour(cx, cy, rad, l);
            if (containedIn(attempt, a)) {
                good_rad = rad;
                rad *= 1.5;
            } else {
                break;
            }
        }// loop for increasing radii
        if (good_rad < 0.0) {
            return null;
        }
        CircleContour sol = new CircleContour(cx, cy, good_rad, l);
        return sol;
    }

    private CircleContour findCircleContour(Rectangle2D.Double box,
            int smallest_rad,
            double guide_rad,
            AbstractBasicRegion zone,
            AbstractDescription last_diag,
            CurveLabel label) throws CannotDrawException {
        ArrayList<CurveLabel> labels = new ArrayList<CurveLabel>();
        labels.add(label);
        ArrayList<CircleContour> result = findCircleContours(box,
                smallest_rad, guide_rad, zone, last_diag, labels);
        if (result == null || result.size() == 0) {
            return null;
        } else {
            return result.get(0);
        }
    }

    private boolean all_ok_in(int lowi, int highi, int lowj, int highj,
            PotentialCentre[][] ok_array, int Ni, int Nj) {
        boolean all_ok = true;
        for (int i = lowi; all_ok && i < highi + 1; i++) {
            for (int j = lowj; all_ok && j < highj + 1; j++) {
                if (i >= Ni || j >= Nj || !ok_array[i][j].ok) {
                    all_ok = false;
                }
            }
        }
        return all_ok;
    }

    private ArrayList<CircleContour> findCircleContours(Rectangle2D.Double box,
            int smallest_rad,
            double guide_rad,
            AbstractBasicRegion zone,
            AbstractDescription last_diag,
            ArrayList<CurveLabel> labels) throws CannotDrawException {
        ArrayList<CircleContour> result = new ArrayList<CircleContour>();

        // special case : our first contour
        boolean is_first_contour = !map.keySet().iterator().hasNext();
        if (is_first_contour) {
            int label_index = 0;
            for (CurveLabel l : labels) {
                result.add(new CircleContour(
                        box.getCenterX() - 0.5 * (guide_rad * 3 * labels.size()) + 1.5 * guide_rad
                        + guide_rad * 3 * label_index,
                        box.getCenterY(),
                        guide_rad, l));
                label_index++;
            }
            DEB.out(2, "added first contours into diagram, labelled " + labels.get(0).getLabel());
            return result;
        }

        if (zone.getNumContours() == 0) {
            // adding a contour outside everything else
            double minx = Double.MAX_VALUE;
            double maxx = Double.MIN_VALUE;
            double miny = Double.MAX_VALUE;
            double maxy = Double.MIN_VALUE;

            for (CircleContour c : circles) {
                if (c.getMinX() < minx) {
                    minx = c.getMinX();
                }
                if (c.getMaxX() > maxx) {
                    maxx = c.getMaxX();
                }
                if (c.getMinY() < miny) {
                    miny = c.getMinY();
                }
                if (c.getMaxY() > maxy) {
                    maxy = c.getMaxY();
                }
            }
            if (labels.size() == 1) {
                if (maxx - minx < maxy - miny) {// R
                    result.add(new CircleContour(
                            maxx + guide_rad * 1.5,
                            (miny + maxy) * 0.5,
                            guide_rad, labels.get(0)));
                } else {// B
                    result.add(new CircleContour(
                            (minx + maxx) * 0.5,
                            maxy + guide_rad * 1.5,
                            guide_rad, labels.get(0)));
                }
            } else if (labels.size() == 2) {
                if (maxx - minx < maxy - miny) {// R
                    result.add(new CircleContour(
                            maxx + guide_rad * 1.5,
                            (miny + maxy) * 0.5,
                            guide_rad, labels.get(0)));
                    result.add(new CircleContour(
                            minx - guide_rad * 1.5,
                            (miny + maxy) * 0.5,
                            guide_rad, labels.get(1)));
                } else {// T
                    result.add(new CircleContour(
                            (minx + maxx) * 0.5,
                            maxy + guide_rad * 1.5,
                            guide_rad, labels.get(0)));
                    result.add(new CircleContour(
                            (minx + maxx) * 0.5,
                            miny - guide_rad * 1.5,
                            guide_rad, labels.get(1)));
                }
            } else {
                if (maxx - minx < maxy - miny) {// R
                    double lowy = (miny + maxy) * 0.5 - 0.5 * labels.size() * guide_rad * 3 + guide_rad * 1.5;
                    for (int i = 0; i < labels.size(); i++) {
                        result.add(new CircleContour(
                                maxx + guide_rad * 1.5,
                                lowy + i * 3 * guide_rad,
                                guide_rad, labels.get(i)));
                    }
                } else {
                    double lowx = (minx + maxx) * 0.5 - 0.5 * labels.size() * guide_rad * 3 + guide_rad * 1.5;
                    for (int i = 0; i < labels.size(); i++) {
                        result.add(new CircleContour(
                                lowx + i * 3 * guide_rad,
                                maxy + guide_rad * 1.5,
                                guide_rad, labels.get(i)));
                    }
                }
            }
            return result;
        }

        ConcreteZone cz = makeConcreteZone(zone);
        Area a = new Area(cz.getShape(box));
        if (a.isEmpty()) {
            throw new CannotDrawException("cannot put a nested contour into an empty region");
        }

        // special case : one contour inside another with no other interference between
        // look at the final diagram - find the corresponding zone
        DEB.out(2, "");
        if (zone.getNumContours() > 0 && labels.size() == 1) {
            //System.out.println("look for "+zone.debug()+" in "+last_diag.debug());
            // not the outside zone - locate the zone in the last diag
            AbstractBasicRegion zoneInLast = null;
            Iterator<AbstractBasicRegion> abrIt = last_diag.getZoneIterator();
            while (abrIt.hasNext() && zoneInLast == null) {
                AbstractBasicRegion abrInLast = abrIt.next();
                if (abrInLast.isLabelEquivalent(zone)) {
                    zoneInLast = abrInLast;
                }
            }
            DEB.assertCondition(zoneInLast != null, "failed to locate zone in final diagram");

            // how many neighbouring abrs?
            abrIt = last_diag.getZoneIterator();
            ArrayList<AbstractCurve> nbring_curves = new ArrayList<AbstractCurve>();
            while (abrIt.hasNext()) {
                AbstractBasicRegion abrInLast = abrIt.next();
                AbstractCurve ac = zoneInLast.getStraddledContour(abrInLast);
                if (ac != null) {
                    if (ac.getLabel() != labels.get(0)) {
                        nbring_curves.add(ac);
                    }
                }
            }
            if (nbring_curves.size() == 1) {
                //  we should use concentric circles

                AbstractCurve acOutside = nbring_curves.get(0);
                // use the centre of the relevant contour
                DEB.assertCondition(acOutside != null, "did not find containing contour");
                CircleContour ccOutside = map.get(acOutside);
                DEB.assertCondition(ccOutside != null, "did not find containing circle");
                if (ccOutside != null) {
                    DEB.out(2, "putting contour " + labels.get(0) + " inside " + acOutside.getLabel());
                    double rad = Math.min(guide_rad, ccOutside.radius - smallest_rad);
                    if (rad > 0.99 * smallest_rad) {
                        // build a co-centric contour
                        CircleContour attempt = new CircleContour(
                                ccOutside.cx, ccOutside.cy, rad, labels.get(0));
                        if (containedIn(attempt, a)) {
                            if (rad > 2 * smallest_rad) // shrink the co-centric contour a bit
                            {
                                attempt = new CircleContour(
                                        ccOutside.cx, ccOutside.cy, rad - smallest_rad, labels.get(0));
                            }
                            result.add(attempt);
                            return result;
                        }
                    }
                } else {
                    System.out.println("warning : did not find expected containing circle...");
                }
            } else if (nbring_curves.size() == 2) {
                //  we should put a circle along the line between two existing centres
                AbstractCurve ac1 = nbring_curves.get(0);
                AbstractCurve ac2 = nbring_curves.get(1);

                CircleContour cc1 = map.get(ac1);
                CircleContour cc2 = map.get(ac2);

                if (cc1 != null && cc2 != null) {
                    boolean in1 = zone.is_in(ac1);
                    boolean in2 = zone.is_in(ac2);

                    double step_c1_c2_x = cc2.cx - cc1.cx;
                    double step_c1_c2_y = cc2.cy - cc1.cy;

                    double step_c1_c2_len = Math.sqrt(step_c1_c2_x * step_c1_c2_x
                            + step_c1_c2_y * step_c1_c2_y);
                    double unit_c1_c2_x = 1.0;
                    double unit_c1_c2_y = 0.0;
                    if (step_c1_c2_len != 0.0) {
                        unit_c1_c2_x = step_c1_c2_x / step_c1_c2_len;
                        unit_c1_c2_y = step_c1_c2_y / step_c1_c2_len;
                    }

                    double p1x = cc1.cx + unit_c1_c2_x * cc1.radius * (in2 ? 1.0 : -1.0);
                    double p2x = cc2.cx + unit_c1_c2_x * cc2.radius * (in1 ? -1.0 : +1.0);
                    double cx = (p1x + p2x) * 0.5;
                    double max_radx = (p2x - p1x) * 0.5;
                    double p1y = cc1.cy + unit_c1_c2_y * cc1.radius * (in2 ? 1.0 : -1.0);
                    double p2y = cc2.cy + unit_c1_c2_y * cc2.radius * (in1 ? -1.0 : +1.0);
                    double cy = (p1y + p2y) * 0.5;
                    double max_rady = (p2y - p1y) * 0.5;
                    double max_rad = Math.sqrt(max_radx * max_radx + max_rady * max_rady);

                    // build a contour
                    CircleContour attempt = new CircleContour(
                            cx, cy, max_rad - smallest_rad, labels.get(0));
                    //DEB.show(attempt.getBigInterior());
                    if (containedIn(attempt, a)) {
                        if (max_rad > 3 * smallest_rad) // shrink the co-centric contour a bit
                        {
                            attempt = new CircleContour(
                                    cx, cy, max_rad - 2 * smallest_rad, labels.get(0));
                        } else if (max_rad > 2 * smallest_rad) // shrink the co-centric contour a bit
                        {
                            attempt = new CircleContour(
                                    cx, cy, max_rad - smallest_rad, labels.get(0));
                        }
                        result.add(attempt);
                        return result;
                    }
                }
            }
        }

        // special case - inserting a nested contour into a part of a Venn2


        Rectangle bounds = a.getBounds();
        /*
        // try from the middle of the bounds.
        double cx = bounds.getCenterX();
        double cy = bounds.getCenterX();
        if(a.contains(cx, cy))
        {
        if(labels.size() == 1)
        {
        // go for a circle of the suggested size
        CircleContour attempt = new CircleContour(cx, cy, guide_rad, labels.get(0));
        if(containedIn(attempt, a))
        {
        result.add(attempt);
        return result;
        }
        }
        else
        {
        Rectangle box = new Rectangle(cx - guide_rad/2)
        }
        }
         */
        if(labels.get(0) == null)
        	DEB.out(2, "putting unlabelled contour inside a zone - grid-style");
        else
        	DEB.out(2, "putting contour " + labels.get(0).getLabel() + " inside a zone - grid-style");

        // Use a grid approach to search for a space for the contour(s)
        int ni = (int) (bounds.getWidth() / smallest_rad) + 1;
        int nj = (int) (bounds.getHeight() / smallest_rad) + 1;
        PotentialCentre contained[][] = new PotentialCentre[ni][nj];
        double basex = bounds.getMinX();
        double basey = bounds.getMinY();
        if (DEB.level > 3) {
            System.out.println("--------");
        }
        for (int i = 0; i < ni; i++) {
            double cx = basex + i * smallest_rad;

            for (int j = 0; j < nj; j++) {
                double cy = basey + j * smallest_rad;
                //System.out.println("check for ("+cx+","+cy+") in region");
                contained[i][j] = new PotentialCentre(cx, cy, a.contains(cx, cy));
                if (DEB.level > 3) {
                    if (contained[i][j].ok) {
                        System.out.print("o");
                    } else {
                        System.out.print("x");
                    }
                }
            }
            if (DEB.level > 3) {
                System.out.println("");
            }
        }
        if (DEB.level > 3) {
            System.out.println("--------");
        }
        // look in contained[] for a large square

        int corneri = -1, cornerj = -1, size = -1;
        boolean isTall = true; // or isWide
        for (int i = 0; i < ni; i++) {
            for (int j = 0; j < nj; j++) {
                // biggest possible square?
                int max_sq = Math.min(ni - i, nj - j);
                for (int sq = size + 1; sq < max_sq + 1; sq++) {
                    // scan a square from i, j
                    DEB.out(2, "look for a box from (" + i + "," + j + ") size " + sq);

                    if (all_ok_in(i, i + (sq * labels.size()) + 1, j, j + sq + 1, contained, ni, nj)) {
                        DEB.out(2, "found a wide box, corner at (" + i + "," + j + "), size " + sq);
                        corneri = i;
                        cornerj = j;
                        size = sq;
                        isTall = false;
                    } else if (labels.size() > 1
                            && all_ok_in(i, i + sq + 1, j, j + (sq * labels.size()) + 1, contained, ni, nj)) {
                        DEB.out(2, "found a tall box, corner at (" + i + "," + j + "), size " + sq);
                        corneri = i;
                        cornerj = j;
                        size = sq;
                        isTall = true;
                    } else {
                        break; // neither wide nor tall worked - move onto next (x, y)
                    }
                }// loop for increasing sizes
            }// loop for j corner
        }// loop for i corner
        //System.out.println("best square is at corner ("+corneri+","+cornerj+"), of size "+size);
        if (size > 0) {
            PotentialCentre pc = contained[corneri][cornerj];
            double radius = size * smallest_rad * 0.5;
            double actualRad = radius;
            if (actualRad > 2 * smallest_rad) {
                actualRad -= smallest_rad;
            } else if (actualRad > smallest_rad) {
                actualRad = smallest_rad;
            }

            // have size, cx, cy
            DEB.out(2, "corner at " + pc.x + "," + pc.y + ", size " + size);

            ArrayList<CircleContour> centredCircles = new ArrayList<CircleContour>();

            double bx = bounds.getCenterX();
            double by = bounds.getCenterY();
            if (isTall) {
                by -= radius * (labels.size() - 1);
            } else {
                bx -= radius * (labels.size() - 1);
            }
            for (int labelIndex = 0;
                    centredCircles != null && labelIndex < labels.size();
                    labelIndex++) {
                CurveLabel l = labels.get(labelIndex);
                double x = bx;
                double y = by;
                if (isTall) {
                    y += 2 * radius * labelIndex;
                } else {
                    x += 2 * radius * labelIndex;
                }

                CircleContour attempt = new CircleContour(x, y,
                        Math.min(guide_rad, actualRad), l);
                //DEB.show(attempt.getBigInterior());
                if (containedIn(attempt, a)) {
                    centredCircles.add(attempt);
                } else {
                    centredCircles = null;
                    //Debug.show(a);
                }
            }
            if (centredCircles != null) {
                result.addAll(centredCircles);
                return result;
            }

            for (int labelIndex = 0; labelIndex < labels.size(); labelIndex++) {
                CurveLabel l = labels.get(labelIndex);
                double x = pc.x + radius;
                double y = pc.y + radius;
                if (isTall) {
                    y += 2 * radius * labelIndex;
                } else {
                    x += 2 * radius * labelIndex;
                }

                CircleContour attempt = new CircleContour(x, y,
                        Math.min(guide_rad, actualRad + smallest_rad), l);
                if (containedIn(attempt, a)) {
                    result.add(attempt);
                } else {
                    result.add(new CircleContour(x, y, actualRad, l));
                }
            }
            return result;
        } else {
            throw new CannotDrawException("cannot fit nested contour into region");
        }
    }

    private double[][] intersect(double c1x, double c1y, double rad1,
            double c2x, double c2y, double rad2) {

        double ret[][] = new double[2][2];
        double dx = c1x - c2x;
        double dy = c1y - c2y;
        double d2 = dx * dx + dy * dy;
        double d = Math.sqrt(d2);

        if (d > rad1 + rad2 || d < Math.abs(rad1 - rad2)) {
            return null; // no solution
        }

        double a = (rad1 * rad1 - rad2 * rad2 + d2) / (2 * d);
        double h = Math.sqrt(rad1 * rad1 - a * a);
        double x2 = c1x + a * (c2x - c1x) / d;
        double y2 = c1y + a * (c2y - c1y) / d;


        double paX = x2 + h * (c2y - c1y) / d;
        double paY = y2 - h * (c2x - c1x) / d;
        double pbX = x2 - h * (c2y - c1y) / d;
        double pbY = y2 + h * (c2x - c1x) / d;

        ret[0][0] = paX;
        ret[0][1] = paY;
        ret[1][0] = pbX;
        ret[1][1] = pbY;

        return ret;
    }

    private boolean containedIn(CircleContour c, Area a) {
        Area test = new Area(c.getFatInterior(smallest_rad));
        test.subtract(a);
        return test.isEmpty();
    }
}

class PotentialCentre {

    double x;
    double y;
    boolean ok;

    PotentialCentre(double x, double y, boolean ok) {
        this.x = x;
        this.y = y;
        this.ok = ok;
    }
}
