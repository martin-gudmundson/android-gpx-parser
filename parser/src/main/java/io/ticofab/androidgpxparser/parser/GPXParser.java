package io.ticofab.androidgpxparser.parser;

import android.util.Xml;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.ISODateTimeFormat;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import io.ticofab.androidgpxparser.parser.domain.Author;
import io.ticofab.androidgpxparser.parser.domain.Bounds;
import io.ticofab.androidgpxparser.parser.domain.Copyright;
import io.ticofab.androidgpxparser.parser.domain.Email;
import io.ticofab.androidgpxparser.parser.domain.Extension;
import io.ticofab.androidgpxparser.parser.domain.Gpx;
import io.ticofab.androidgpxparser.parser.domain.Link;
import io.ticofab.androidgpxparser.parser.domain.Metadata;
import io.ticofab.androidgpxparser.parser.domain.Point;
import io.ticofab.androidgpxparser.parser.domain.Route;
import io.ticofab.androidgpxparser.parser.domain.RoutePoint;
import io.ticofab.androidgpxparser.parser.domain.Track;
import io.ticofab.androidgpxparser.parser.domain.TrackPoint;
import io.ticofab.androidgpxparser.parser.domain.TrackSegment;
import io.ticofab.androidgpxparser.parser.domain.WayPoint;
import io.ticofab.androidgpxparser.parser.domain.XMLAttribute;

public class GPXParser {

    static private final String TAG_GPX = "gpx";
    static private final String TAG_VERSION = "version";
    static private final String TAG_CREATOR = "creator";
    static private final String TAG_METADATA = "metadata";
    static private final String TAG_TRACK = "trk";
    static private final String TAG_SEGMENT = "trkseg";
    static private final String TAG_TRACK_POINT = "trkpt";
    static private final String TAG_LAT = "lat";
    static private final String TAG_LON = "lon";
    static private final String TAG_ELEVATION = "ele";
    static private final String TAG_TIME = "time";
    static private final String TAG_SYM = "sym";
    static private final String TAG_WAY_POINT = "wpt";
    static private final String TAG_ROUTE = "rte";
    static private final String TAG_ROUTE_POINT = "rtept";
    static private final String TAG_NAME = "name";
    static private final String TAG_DESC = "desc";
    static private final String TAG_CMT = "cmt";
    static private final String TAG_SRC = "src";
    static private final String TAG_LINK = "link";
    static private final String TAG_NUMBER = "number";
    static private final String TAG_TYPE = "type";
    static private final String TAG_TEXT = "text";
    static private final String TAG_AUTHOR = "author";
    static private final String TAG_COPYRIGHT = "copyright";
    static private final String TAG_KEYWORDS = "keywords";
    static private final String TAG_BOUNDS = "bounds";
    static private final String TAG_MIN_LAT = "minlat";
    static private final String TAG_MIN_LON = "minlon";
    static private final String TAG_MAX_LAT = "maxlat";
    static private final String TAG_MAX_LON = "maxlon";
    static private final String TAG_HREF = "href";
    static private final String TAG_YEAR = "year";
    static private final String TAG_LICENSE = "license";
    static private final String TAG_EMAIL = "email";
    static private final String TAG_ID = "id";
    static private final String TAG_DOMAIN = "domain";

    // extensions-related tags
    static private final String TAG_EXTENSIONS = "extensions";
    static private final String TAG_SPEED = "speed";

    static private final String namespace = null;

    public Gpx parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            parser.setInput(in, null);
            parser.nextTag();
            return readGpx(parser);
        } finally {
            in.close();
        }
    }

    public void write(Gpx gpx, OutputStream out) throws IOException, IllegalArgumentException, IllegalStateException {
        try {
            XmlSerializer serializer = Xml.newSerializer();
            serializer.setOutput(out, "UTF-8");
            serializer.startDocument("UTF-8", false);
            writeGpx(gpx, serializer);
            serializer.endDocument();
        } finally {
            out.close();
        }

    }

    private Gpx readGpx(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<WayPoint> wayPoints = new ArrayList<>();
        List<Track> tracks = new ArrayList<>();
        List<Route> routes = new ArrayList<>();

        parser.require(XmlPullParser.START_TAG, namespace, TAG_GPX);

        Gpx.Builder builder = new Gpx.Builder();
        builder.setVersion(parser.getAttributeValue(namespace, TAG_VERSION));
        builder.setCreator(parser.getAttributeValue(namespace, TAG_CREATOR));
        builder.setAttributes(readAttributes(parser));


        while (loopMustContinue(parser.next())) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            switch (name) {
                case TAG_METADATA:
                    builder.setMetadata(readMetadata(parser));
                    break;
                case TAG_WAY_POINT:
                    wayPoints.add(readWayPoint(parser));
                    break;
                case TAG_ROUTE:
                    routes.add(readRoute(parser));
                    break;
                case TAG_TRACK:
                    tracks.add(readTrack(parser));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        parser.require(XmlPullParser.END_TAG, namespace, TAG_GPX);
        return builder
                .setWayPoints(wayPoints)
                .setRoutes(routes)
                .setTracks(tracks)
                .build();
    }

    // Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them off
    // to their respective "read" methods for processing. Otherwise, skips the tag.
    private Track readTrack(XmlPullParser parser) throws XmlPullParserException, IOException {
        Track.Builder trackBuilder = new Track.Builder();

        List<TrackSegment> segments = new ArrayList<>();
        parser.require(XmlPullParser.START_TAG, namespace, TAG_TRACK);
        while (loopMustContinue(parser.next())) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case TAG_NAME:
                    trackBuilder.setTrackName(readName(parser));
                    break;
                case TAG_SEGMENT:
                    segments.add(readSegment(parser));
                    break;
                case TAG_DESC:
                    trackBuilder.setTrackDesc(readDesc(parser));
                    break;
                case TAG_CMT:
                    trackBuilder.setTrackCmt(readCmt(parser));
                    break;
                case TAG_SRC:
                    trackBuilder.setTrackSrc(readString(parser, TAG_SRC));
                    break;
                case TAG_LINK:
                    trackBuilder.setTrackLink(readLink(parser));
                    break;
                case TAG_NUMBER:
                    trackBuilder.setTrackNumber(readNumber(parser));
                    break;
                case TAG_TYPE:
                    trackBuilder.setTrackType(readString(parser, TAG_TYPE));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        parser.require(XmlPullParser.END_TAG, namespace, TAG_TRACK);
        return trackBuilder
                .setTrackSegments(segments)
                .build();
    }

    private Link readLink(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, namespace, TAG_LINK);

        Link.Builder linkBuilder = new Link.Builder();
        linkBuilder.setLinkHref(parser.getAttributeValue(namespace, TAG_HREF));

        while (loopMustContinue(parser.next())) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case TAG_TEXT:
                    linkBuilder.setLinkText(readString(parser, TAG_TEXT));
                    break;
                case TAG_TYPE:
                    linkBuilder.setLinkType(readString(parser, TAG_TYPE));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        parser.require(XmlPullParser.END_TAG, namespace, TAG_LINK);
        return linkBuilder.build();
    }

    private Bounds readBounds(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, namespace, TAG_BOUNDS);
        Bounds bounds = new Bounds.Builder()
                .setMinLat(Double.valueOf(parser.getAttributeValue(namespace, TAG_MIN_LAT)))
                .setMinLon(Double.valueOf(parser.getAttributeValue(namespace, TAG_MIN_LON)))
                .setMaxLat(Double.valueOf(parser.getAttributeValue(namespace, TAG_MAX_LAT)))
                .setMaxLon(Double.valueOf(parser.getAttributeValue(namespace, TAG_MAX_LON)))
                .build();

        parser.nextTag();

        parser.require(XmlPullParser.END_TAG, namespace, TAG_BOUNDS);

        return bounds;
    }

    // Processes summary tags in the feed.
    private TrackSegment readSegment(XmlPullParser parser) throws IOException, XmlPullParserException {
        List<TrackPoint> points = new ArrayList<>();
        List<Extension> extensions = new ArrayList<>();
        parser.require(XmlPullParser.START_TAG, namespace, TAG_SEGMENT);
        while (loopMustContinue(parser.next())) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case TAG_TRACK_POINT:
                    points.add(readTrackPoint(parser));
                    break;
                case TAG_EXTENSIONS:
                    extensions.addAll(readExtensions(parser));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        parser.require(XmlPullParser.END_TAG, namespace, TAG_SEGMENT);
        return new TrackSegment.Builder()
                .setTrackPoints(points)
                .setExtensions(extensions)
                .build();
    }

    private Route readRoute(XmlPullParser parser) throws IOException, XmlPullParserException {
        List<RoutePoint> points = new ArrayList<>();
        parser.require(XmlPullParser.START_TAG, namespace, TAG_ROUTE);
        Route.Builder routeBuilder = new Route.Builder();

        while (loopMustContinue(parser.next())) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case TAG_ROUTE_POINT:
                    points.add(readRoutePoint(parser));
                    break;
                case TAG_NAME:
                    routeBuilder.setRouteName(readName(parser));
                    break;
                case TAG_DESC:
                    routeBuilder.setRouteDesc(readDesc(parser));
                    break;
                case TAG_CMT:
                    routeBuilder.setRouteCmt(readCmt(parser));
                    break;
                case TAG_SRC:
                    routeBuilder.setRouteSrc(readString(parser, TAG_SRC));
                    break;
                case TAG_LINK:
                    routeBuilder.setRouteLink(readLink(parser));
                    break;
                case TAG_NUMBER:
                    routeBuilder.setRouteNumber(readNumber(parser));
                    break;
                case TAG_TYPE:
                    routeBuilder.setRouteType(readString(parser, TAG_TYPE));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        parser.require(XmlPullParser.END_TAG, namespace, TAG_ROUTE);
        return routeBuilder
                .setRoutePoints(points)
                .build();
    }

    /**
     * Reads a single point, which can either be a {@link TrackPoint}, {@link RoutePoint} or {@link WayPoint}.
     *
     * @param builder The prepared builder, one of {@link TrackPoint.Builder}, {@link RoutePoint.Builder} or {@link WayPoint.Builder}.
     * @param parser  Parser
     * @param tagName Tag name, e.g. trkpt, rtept, wpt
     */
    private Point readPoint(Point.Builder builder, XmlPullParser parser, String tagName) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, namespace, tagName);

        builder.setLatitude(Double.valueOf(parser.getAttributeValue(namespace, TAG_LAT)));
        builder.setLongitude(Double.valueOf(parser.getAttributeValue(namespace, TAG_LON)));

        while (loopMustContinue(parser.next())) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case TAG_NAME:
                    builder.setName(readName(parser));
                    break;
                case TAG_DESC:
                    builder.setDesc(readDesc(parser));
                    break;
                case TAG_ELEVATION:
                    builder.setElevation(readElevation(parser));
                    break;
                case TAG_TIME:
                    builder.setTime(readTime(parser));
                    break;
                case TAG_TYPE:
                    builder.setType(readType(parser));
                    break;
                case TAG_EXTENSIONS:
                    builder.setExtensions(readExtensions(parser));
                    break;
                case TAG_SYM:
                    builder.setSym(readSym(parser));
                    break;
                case TAG_CMT:
                    builder.setCmt(readCmt(parser));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }

        parser.require(XmlPullParser.END_TAG, namespace, tagName);
        return builder.build();
    }

    private Metadata readMetadata(XmlPullParser parser) throws XmlPullParserException, IOException {
        Metadata.Builder metadataBuilder = new Metadata.Builder();

        parser.require(XmlPullParser.START_TAG, namespace, TAG_METADATA);
        while (loopMustContinue(parser.next())) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case TAG_NAME:
                    metadataBuilder.setName(readName(parser));
                    break;
                case TAG_DESC:
                    metadataBuilder.setDesc(readDesc(parser));
                    break;
                case TAG_AUTHOR:
                    metadataBuilder.setAuthor(readAuthor(parser));
                    break;
                case TAG_COPYRIGHT:
                    metadataBuilder.setCopyright(readCopyright(parser));
                    break;
                case TAG_LINK:
                    metadataBuilder.setLink(readLink(parser));
                    break;
                case TAG_TIME:
                    metadataBuilder.setTime(readTime(parser));
                    break;
                case TAG_KEYWORDS:
                    metadataBuilder.setKeywords(readString(parser, TAG_KEYWORDS));
                    break;
                case TAG_BOUNDS:
                    metadataBuilder.setBounds(readBounds(parser));
                    break;
                case TAG_EXTENSIONS:
                default:
                    skip(parser);
                    break;
            }
        }
        parser.require(XmlPullParser.END_TAG, namespace, TAG_METADATA);
        return metadataBuilder.build();
    }

    private Author readAuthor(XmlPullParser parser) throws XmlPullParserException, IOException {
        Author.Builder authorBuilder = new Author.Builder();

        parser.require(XmlPullParser.START_TAG, namespace, TAG_AUTHOR);
        while (loopMustContinue(parser.next())) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case TAG_NAME:
                    authorBuilder.setName(readString(parser, TAG_NAME));
                    break;
                case TAG_EMAIL:
                    authorBuilder.setEmail(readEmail(parser));
                    break;
                case TAG_LINK:
                    authorBuilder.setLink(readLink(parser));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        parser.require(XmlPullParser.END_TAG, namespace, TAG_AUTHOR);
        return authorBuilder.build();
    }

    private Email readEmail(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, namespace, TAG_EMAIL);

        Email.Builder emailBuilder = new Email.Builder();
        emailBuilder.setId(parser.getAttributeValue(namespace, TAG_ID));
        emailBuilder.setDomain(parser.getAttributeValue(namespace, TAG_DOMAIN));

        // Email tag is self closed, advance the parser to next event
        parser.next();

        parser.require(XmlPullParser.END_TAG, namespace, TAG_EMAIL);
        return emailBuilder.build();
    }

    private Copyright readCopyright(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, namespace, TAG_COPYRIGHT);

        Copyright.Builder copyrightBuilder = new Copyright.Builder();
        copyrightBuilder.setAuthor(parser.getAttributeValue(namespace, TAG_AUTHOR));

        while (loopMustContinue(parser.next())) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case TAG_YEAR:
                    copyrightBuilder.setYear(readYear(parser));
                    break;
                case TAG_LICENSE:
                    copyrightBuilder.setLicense(readString(parser, TAG_LICENSE));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        parser.require(XmlPullParser.END_TAG, namespace, TAG_COPYRIGHT);
        return copyrightBuilder.build();
    }

    private WayPoint readWayPoint(XmlPullParser parser) throws XmlPullParserException, IOException {
        return (WayPoint) readPoint(new WayPoint.Builder(), parser, TAG_WAY_POINT);
    }

    private TrackPoint readTrackPoint(XmlPullParser parser) throws IOException, XmlPullParserException {
        return (TrackPoint) readPoint(new TrackPoint.Builder(), parser, TAG_TRACK_POINT);
    }

    private RoutePoint readRoutePoint(XmlPullParser parser) throws IOException, XmlPullParserException {
        return (RoutePoint) readPoint(new RoutePoint.Builder(), parser, TAG_ROUTE_POINT);
    }

    private String readName(XmlPullParser parser) throws IOException, XmlPullParserException {
        return readString(parser, TAG_NAME);
    }

    private String readDesc(XmlPullParser parser) throws IOException, XmlPullParserException {
        return readString(parser, TAG_DESC);
    }

    private String readType(XmlPullParser parser) throws IOException, XmlPullParserException {
        return readString(parser, TAG_TYPE);
    }

    private String readCmt(XmlPullParser parser) throws IOException, XmlPullParserException {
        return readString(parser, TAG_CMT);
    }

    private String readString(XmlPullParser parser, String tag) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, namespace, tag);
        String value = readText(parser);
        parser.require(XmlPullParser.END_TAG, namespace, tag);
        return value;
    }

    private Double readElevation(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, namespace, TAG_ELEVATION);
        Double ele = Double.valueOf(readText(parser));
        parser.require(XmlPullParser.END_TAG, namespace, TAG_ELEVATION);
        return ele;
    }

    private DateTime readTime(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, namespace, TAG_TIME);
        DateTime time = ISODateTimeFormat.dateTimeParser().parseDateTime(readText(parser));
        parser.require(XmlPullParser.END_TAG, namespace, TAG_TIME);
        return time;
    }

    private String readSym(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, namespace, TAG_SYM);
        String value = readText(parser);
        parser.require(XmlPullParser.END_TAG, namespace, TAG_SYM);
        return value;
    }

    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    private Integer readNumber(XmlPullParser parser) throws IOException, XmlPullParserException, NumberFormatException {
        parser.require(XmlPullParser.START_TAG, namespace, TAG_NUMBER);
        Integer number = Integer.valueOf(readText(parser));
        parser.require(XmlPullParser.END_TAG, namespace, TAG_NUMBER);
        return number;
    }

    private Double readSpeed(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, namespace, TAG_SPEED);
        double speed;
        try {
            speed = Double.parseDouble(readText(parser));
        } catch (NumberFormatException e) {
            // there was an issue parsing speed, default to 0.0
            speed = 0.0;
        }
        parser.require(XmlPullParser.END_TAG, namespace, TAG_SPEED);
        return speed;
    }

    private Integer readYear(XmlPullParser parser) throws IOException, XmlPullParserException, NumberFormatException {
        parser.require(XmlPullParser.START_TAG, namespace, TAG_YEAR);
        String yearStr = readText(parser);

        // we might need to strip an optional time-zone, even though I've never seen it
        // "2019" vs "2019+05:00" or "2019-03:00"
        int timeZoneStart = yearStr.indexOf('+');
        if (timeZoneStart == -1) timeZoneStart = yearStr.indexOf('-');
        yearStr = (timeZoneStart == -1) ? yearStr : yearStr.substring(0, timeZoneStart);

        Integer year = Integer.valueOf(yearStr);
        parser.require(XmlPullParser.END_TAG, namespace, TAG_YEAR);
        return year;
    }

    private List<Extension> readExtensions(XmlPullParser parser) throws IOException, XmlPullParserException {
        List<Extension> extensions = new ArrayList<>();

        parser.require(XmlPullParser.START_TAG, namespace, TAG_EXTENSIONS);
        while (loopMustContinue(parser.next())) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            extensions.addAll(readChildExtensions(parser));
        }
        parser.require(XmlPullParser.END_TAG, namespace, TAG_EXTENSIONS);

        return extensions;
    }

    private List<Extension> readChildExtensions(XmlPullParser parser) throws IOException, XmlPullParserException {
        List<Extension> extensions = new ArrayList<>();
        while (parser.getEventType() == XmlPullParser.START_TAG) {
            extensions.add(readExtension(parser));
        }

        return extensions;
    }

    private Extension readExtension(XmlPullParser parser) throws IOException, XmlPullParserException {

        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IOException("Expecting readExtension to already be in a START_TAG event type");
        }

        Extension.Builder extensionBuilder = new Extension.Builder();
        extensionBuilder.setName(parser.getName());
        extensionBuilder.setPrefix(parser.getPrefix());
        extensionBuilder.setNamespace(parser.getNamespace(parser.getPrefix()));
        extensionBuilder.setAttributes(readAttributes(parser));
        extensionBuilder.setValue(readText(parser));

        switch (parser.getEventType()) {
            case XmlPullParser.END_TAG:
                extensionBuilder.setChildren(new ArrayList<>());
                parser.nextTag();
                break;
            case XmlPullParser.START_TAG:
                extensionBuilder.setChildren(readChildExtensions(parser));
                break;
        }

        return extensionBuilder.build();
    }

    private List<XMLAttribute> readAttributes(XmlPullParser parser) {
        List<XMLAttribute> attributes = new ArrayList<>();
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            attributes.add(new XMLAttribute.Builder()
                    .setName(parser.getAttributeName(i))
                    .setValue(parser.getAttributeValue(i))
                    .setType(parser.getAttributeType(i))
                    .setPrefix(parser.getAttributePrefix(i))
                    .setNamespace(parser.getAttributeNamespace(i))
                    .build()
            );
        }
        return attributes;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    private boolean loopMustContinue(int next) {
        return next != XmlPullParser.END_TAG && next != XmlPullParser.END_DOCUMENT;
    }

    private void writeGpx(Gpx gpx, XmlSerializer serializer) throws IOException, IllegalStateException, IllegalArgumentException {

        // Need to set namespaces before start tag
        serializer.setPrefix("", gpx.getNamespace());
        writePrefixes(gpx.getAttributes(), serializer);
        serializer.startTag(gpx.getNamespace(), TAG_GPX);

        // Set attributes for GPX Tag
        serializer.attribute(null, TAG_CREATOR, gpx.getCreator());
        serializer.attribute(null, TAG_VERSION, gpx.getVersion());

        List<String> mSkipKeys = new ArrayList<>();
        mSkipKeys.add(TAG_CREATOR);
        mSkipKeys.add(TAG_VERSION);
        writeAttributes(gpx.getAttributes(), mSkipKeys, serializer);

        // Set Meta data
        writeMetadata(gpx.getMetadata(), gpx.getNamespace(), serializer);
        // Set Waypoints
        writeWayPoints(gpx.getWayPoints(), gpx.getNamespace(), serializer);

        serializer.endTag(gpx.getNamespace(), TAG_GPX);
    }

    private void writePrefixes(List<XMLAttribute> attributes, XmlSerializer serializer) throws IOException, IllegalStateException, IllegalArgumentException {
        for (int i = 0; i < attributes.size(); i++) {
            XMLAttribute attribute = attributes.get(i);
            if (!attribute.getPrefix().isEmpty()) {
                serializer.setPrefix(attribute.getPrefix(), attribute.getNamespace());
            }
        }
    }


    private void writeAttributes(List<XMLAttribute> attributes, List<String> skipKeys, XmlSerializer serializer) throws IOException, IllegalStateException, IllegalArgumentException {
        for (int i = 0; i < attributes.size(); i++) {
            XMLAttribute attribute = attributes.get(i);
            if (skipKeys != null && skipKeys.contains(attribute.getName())) {
                continue;
            }

            serializer.attribute(attribute.getNamespace(), attribute.getName(), attribute.getValue());
            serializer.setPrefix(attribute.getPrefix(), attribute.getNamespace());
        }
    }

    private void writeMetadata(Metadata metadata, String namespace, XmlSerializer serializer) throws IOException, IllegalStateException, IllegalArgumentException {
        if (metadata == null) {
            return;
        }

        serializer.startTag(namespace, TAG_METADATA);
        writeTagWithText(TAG_NAME, metadata.getName(), namespace, serializer);
        writeTagWithText(TAG_DESC, metadata.getDesc(), namespace, serializer);
        writeAuthor(metadata.getAuthor(), namespace, serializer);
        writeCopyright(metadata.getCopyright(), namespace, serializer);
        writeLink(metadata.getLink(), namespace, serializer);
        writeTagWithText(TAG_KEYWORDS, metadata.getKeywords(), namespace, serializer);
        writeTime(metadata.getTime(), namespace, serializer);
        writeBounds(metadata.getBounds(), namespace, serializer);
        // Extensions in metadata not yet supported

        serializer.endTag(namespace, TAG_METADATA);
    }

    private void writeWayPoints(List<WayPoint> wayPoints, String namespace, XmlSerializer serializer) throws IOException, IllegalStateException, IllegalArgumentException {
        if (wayPoints == null) {
            return;
        }
        for (int i = 0; i < wayPoints.size(); i++) {
            writeWayPoint(wayPoints.get(i), namespace, serializer);
        }
    }

    private void writeWayPoint(WayPoint wayPoint, String namespace, XmlSerializer serializer) throws IOException, IllegalStateException, IllegalArgumentException {
        if (wayPoint == null) {
            return;
        }

        serializer.startTag(namespace, TAG_WAY_POINT);
        serializer.attribute(null, TAG_LAT, wayPoint.getLatitude().toString());
        serializer.attribute(null, TAG_LON, wayPoint.getLongitude().toString());

        writeTagWithText(TAG_ELEVATION, wayPoint.getElevation(), namespace, serializer);
        writeTime(wayPoint.getTime(), namespace, serializer);
        writeTagWithText(TAG_NAME, wayPoint.getName(), namespace, serializer);
        writeTagWithText(TAG_DESC, wayPoint.getDesc(), namespace, serializer);
        writeTagWithText(TAG_TYPE, wayPoint.getType(), namespace, serializer);
        writeTagWithText(TAG_SYM, wayPoint.getSym(), namespace, serializer);
        writeTagWithText(TAG_CMT, wayPoint.getCmt(), namespace, serializer);
        writeRootExtensions(wayPoint.getExtensions(), namespace, serializer);

        serializer.endTag(namespace, TAG_WAY_POINT);
    }

    private void writeTagWithText(String tag, Object value, String namespace, XmlSerializer serializer) throws IOException, IllegalStateException, IllegalArgumentException {
        if (value == null) {
            return;
        }

        serializer.startTag(namespace, tag);
        serializer.text(value.toString());
        serializer.endTag(namespace, tag);
    }

    private void writeAuthor(Author author, String namespace, XmlSerializer serializer) throws IOException, IllegalStateException, IllegalArgumentException {
        if (author == null) {
            return;
        }

        serializer.startTag(namespace, TAG_AUTHOR);
        writeTagWithText(TAG_NAME, author.getName(), namespace, serializer);
        writeEmail(author.getEmail(), namespace, serializer);
        writeLink(author.getLink(), namespace, serializer);
        serializer.endTag(namespace, TAG_AUTHOR);

    }

    private void writeEmail(Email email, String namespace, XmlSerializer serializer) throws IOException, IllegalStateException, IllegalArgumentException {
        if (email == null) {
            return;
        }

        serializer.startTag(namespace, TAG_EMAIL);
        serializer.attribute(null, TAG_ID, email.getId());
        serializer.attribute(null, TAG_DOMAIN, email.getDomain());
        serializer.endTag(namespace, TAG_EMAIL);
    }

    private void writeLink(Link link, String namespace, XmlSerializer serializer) throws IOException, IllegalStateException, IllegalArgumentException {
        if (link == null) {
            return;
        }

        serializer.startTag(namespace, TAG_LINK);
        serializer.attribute(null, TAG_HREF, link.getHref());
        writeTagWithText(TAG_TEXT, link.getText(), namespace, serializer);
        writeTagWithText(TAG_TYPE, link.getType(), namespace, serializer);
        serializer.endTag(namespace, TAG_LINK);
    }

    private void writeCopyright(Copyright copyright, String namespace, XmlSerializer serializer) throws IOException, IllegalStateException, IllegalArgumentException {
        if (copyright == null) {
            return;
        }
        serializer.startTag(namespace, TAG_COPYRIGHT);
        serializer.attribute(null, TAG_AUTHOR, copyright.getAuthor());
        writeTagWithText(TAG_YEAR, copyright.getYear(), namespace, serializer);
        writeTagWithText(TAG_LICENSE, copyright.getLicense(), namespace, serializer);
        serializer.endTag(namespace, TAG_COPYRIGHT);
    }

    private void writeTime(DateTime dateTime, String namespace, XmlSerializer serializer) throws IOException, IllegalStateException, IllegalArgumentException {
        if (dateTime == null) {
            return;
        }

        writeTagWithText(TAG_TIME, ISODateTimeFormat.dateTime().print(dateTime), namespace, serializer);
    }

    private void writeBounds(Bounds bounds, String namespace, XmlSerializer serializer) throws IOException, IllegalStateException, IllegalArgumentException {
        if (bounds == null) {
            return;
        }
        serializer.startTag(namespace, TAG_BOUNDS);
        serializer.attribute(null, TAG_MIN_LAT, bounds.getMinLat().toString());
        serializer.attribute(null, TAG_MIN_LON, bounds.getMinLon().toString());
        serializer.attribute(null, TAG_MAX_LAT, bounds.getMaxLat().toString());
        serializer.attribute(null, TAG_MAX_LON, bounds.getMaxLon().toString());
        serializer.endTag(namespace, TAG_BOUNDS);
    }

    private void writeRootExtensions(List<Extension> extensions, String namespace, XmlSerializer serializer) throws IOException, IllegalStateException, IllegalArgumentException {
        if (extensions == null || extensions.size() == 0) {
            return;
        }

        serializer.startTag(namespace, TAG_EXTENSIONS);
        writeExtensions(extensions, serializer);
        serializer.endTag(namespace, TAG_EXTENSIONS);
    }

    private void writeExtensions(List<Extension> extensions, XmlSerializer serializer) throws IOException, IllegalStateException, IllegalArgumentException {
        if (extensions == null || extensions.size() == 0) {
            return;
        }

        for (int i = 0; i < extensions.size(); i++) {
            writeExtension(extensions.get(i), serializer);
        }
    }


    private void writeExtension(Extension extension, XmlSerializer serializer) throws IOException, IllegalStateException, IllegalArgumentException {
        if (extension == null) {
            return;
        }

        writePrefixes(extension.getAttributes(), serializer);
        serializer.setPrefix(extension.getPrefix(), extension.getNamespace());
        serializer.startTag(extension.getNamespace(), extension.getName());
        writeAttributes(extension.getAttributes(), null, serializer);
        if (extension.getValue() != null) {
            serializer.text(extension.getValue());
        }
        writeExtensions(extension.getChildren(), serializer);
        serializer.endTag(extension.getNamespace(), extension.getName());
    }
}