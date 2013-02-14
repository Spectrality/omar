package org.ossim.omar.ogc

import groovy.xml.StreamingMarkupBuilder
import au.com.bytecode.opencsv.CSVWriter
import geoscript.filter.Filter
import geoscript.layer.Layer
import geoscript.layer.io.GeoJSONWriter
import geoscript.workspace.Database
import geoscript.geom.Geometry
import geoscript.workspace.PostGIS
import org.apache.commons.collections.map.CaseInsensitiveMap
import org.apache.commons.io.FilenameUtils

import java.text.SimpleDateFormat

import org.geotools.data.postgis.PostgisNGDataStoreFactory
import org.joda.time.DateTimeZone
import org.joda.time.DateTime
import grails.converters.JSON

class WebFeatureService
{
    static transactional = false

    def grailsLinkGenerator
    def grailsApplication
    def dataSourceUnproxied
    private def wmsPersistParams = ["stretch_mode",
            "stretch_mode_region", "sharpen_width", "sharpen_sigma",
            "sharpen_mode", "width", "height", "format", "srs",
            "service", "version", "request", "quicklook", "bands",
            "transparent", "bgcolor", "styles", "null_flip", "bbox"]
    private def layerNames = [
            'raster_entry',
            'video_data_set'
    ]
    private def typeMappings = [
            'Double': 'xsd:double',
            'Integer': 'xsd:int',
            'Long': 'xsd:long',
            'Polygon': 'gml:PolygonPropertyType',
            'MultiPolygon': 'gml:MultiPolygonPropertyType',
            'String': 'xsd:string',
            'java.lang.Boolean': 'xsd:boolean',
            'java.math.BigDecimal': 'xsd:decimal',
            'java.sql.Timestamp': 'xsd:dateTime',
    ]

    def getCapabilities(def wfsRequest)
    {
        def results, contentType

        def y = {
            mkp.xmlDeclaration()
            mkp.declareNamespace( '': "http://www.opengis.net/wfs" )
            mkp.declareNamespace( ogc: "http://www.opengis.net/ogc" )
            mkp.declareNamespace( omar: "http://omar.ossim.org" )
            mkp.declareNamespace( xsi: "http://www.w3.org/2001/XMLSchema-instance" )

            WFS_Capabilities(
                    version: '1.0.0', updateSequence: '0',
                    'xsi:schemaLocation': "http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.0.0/WFS-capabilities.xsd"
            ) {
                Service {
                    Name( "OMAR" )
                    Title( "OMAR WFS" )
                    Abstract()
                    Keywords()
                    OnlineResource( grailsLinkGenerator.link( base: grailsApplication.config.omar.serverURL, absolute: true, controller: 'wfs' ) )
                    Fees( "NONE" )
                    AccessConstraints( "NONE" )
                }
                Capability {
                    Request {
                        GetCapabilities {
                            DCPType {
                                HTTP {
                                    Get( onlineResource: grailsLinkGenerator.link( base: grailsApplication.config.omar.serverURL, absolute: true, controller: 'wfs'/*, params: [request: 'GetCapabilities']*/ ) )
                                }
                            }
                            DCPType {
                                HTTP {
                                    Post( onlineResource: grailsLinkGenerator.link( absolute: true, controller: 'wfs' ) )
                                }
                            }
                        }
                        DescribeFeatureType {
                            SchemaDescriptionLanguage {
                                XMLSCHEMA()
                            }
                            DCPType {
                                HTTP {
                                    Get( onlineResource: grailsLinkGenerator.link( base: grailsApplication.config.omar.serverURL, absolute: true, controller: 'wfs'/*, params: [request: 'DescribeFeatureType']*/ ) )
                                }
                            }
                            DCPType {
                                HTTP {
                                    Post( onlineResource: grailsLinkGenerator.link( absolute: true, controller: 'wfs' ) )
                                }
                            }
                        }
                        GetFeature {
                            ResultFormat {
                                GML2()
                                //GML3()
                                //'SHAPE-ZIP'()
                                GEOJSON()
                                CSV()
                                KML()
                                KMLQUERY()
                            }
                            DCPType {
                                HTTP {
                                    Get( onlineResource: grailsLinkGenerator.link( base: grailsApplication.config.omar.serverURL, absolute: true, controller: 'wfs'/*, params: [request: 'GetFeature']*/ ) )
                                }
                            }
                            DCPType {
                                HTTP {
                                    Post( onlineResource: grailsLinkGenerator.link( absolute: true, controller: 'wfs' ) )
                                }
                            }
                        }
                    }
                }
                FeatureTypeList {
                    Operations {
                        Query()
                    }
                    def workspace = getWorkspace()
                    for ( def layerName in layerNames )
                    {
                        def layer = workspace[layerName]
                        def bounds = layer.bounds
                        FeatureType {
                            Name( layerName )
                            Title()
                            Abstract()
                            Keywords()
                            SRS( layer?.proj?.id )
                            LatLongBoundingBox( minx: "${ bounds?.minX }", miny: "${ bounds?.minY }",
                                    maxx: "${ bounds?.maxX }", maxy: "${ bounds?.maxY }" )
                        }
                    }
                    workspace?.close()
                }
                ogc.Filter_Capabilities {
                    ogc.Spatial_Capabilities {
                        ogc.Spatial_Operators {
                            ogc.Disjoint()
                            ogc.Equals()
                            ogc.DWithin()
                            ogc.Beyond()
                            ogc.Intersect()
                            ogc.Touches()
                            ogc.Crosses()
                            ogc.Within()
                            ogc.Contains()
                            ogc.Overlaps()
                            ogc.BBOX()
                        }
                    }
                    ogc.Scalar_Capabilities {
                        ogc.Logical_Operators()
                        ogc.Comparison_Operators {
                            ogc.Simple_Comparisons()
                            ogc.Between()
                            ogc.Like()
                            ogc.NullCheck()
                        }
                        ogc.Arithmetic_Operators {
                            ogc.Simple_Arithmetic()
                            ogc.Functions {
                                ogc.Function_Names {
                                    ogc.Function_Name( nArgs: "1", "abs" )
                                    ogc.Function_Name( nArgs: "1", "abs_2" )
                                    ogc.Function_Name( nArgs: "1", "abs_3" )
                                    ogc.Function_Name( nArgs: "1", "abs_4" )
                                    ogc.Function_Name( nArgs: "1", "acos" )
                                    ogc.Function_Name( nArgs: "1", "Area" )
                                    ogc.Function_Name( nArgs: "1", "area2" )
                                    ogc.Function_Name( nArgs: "1", "asin" )
                                    ogc.Function_Name( nArgs: "1", "atan" )
                                    ogc.Function_Name( nArgs: "1", "atan2" )
                                    ogc.Function_Name( nArgs: "3", "between" )
                                    ogc.Function_Name( nArgs: "1", "boundary" )
                                    ogc.Function_Name( nArgs: "1", "boundaryDimension" )
                                    ogc.Function_Name( nArgs: "2", "buffer" )
                                    ogc.Function_Name( nArgs: "3", "bufferWithSegments" )
                                    ogc.Function_Name( nArgs: "7", "Categorize" )
                                    ogc.Function_Name( nArgs: "1", "ceil" )
                                    ogc.Function_Name( nArgs: "1", "centroid" )
                                    ogc.Function_Name( nArgs: "2", "classify" )
                                    ogc.Function_Name( nArgs: "1", "Collection_Average" )
                                    ogc.Function_Name( nArgs: "1", "Collection_Bounds" )
                                    ogc.Function_Name( nArgs: "0", "Collection_Count" )
                                    ogc.Function_Name( nArgs: "1", "Collection_Max" )
                                    ogc.Function_Name( nArgs: "1", "Collection_Median" )
                                    ogc.Function_Name( nArgs: "1", "Collection_Min" )
                                    ogc.Function_Name( nArgs: "1", "Collection_Sum" )
                                    ogc.Function_Name( nArgs: "1", "Collection_Unique" )
                                    ogc.Function_Name( nArgs: "1", "Concatenate" )
                                    ogc.Function_Name( nArgs: "2", "contains" )
                                    ogc.Function_Name( nArgs: "2", "convert" )
                                    ogc.Function_Name( nArgs: "1", "convexHull" )
                                    ogc.Function_Name( nArgs: "1", "cos" )
                                    ogc.Function_Name( nArgs: "2", "crosses" )
                                    ogc.Function_Name( nArgs: "2", "dateFormat" )
                                    ogc.Function_Name( nArgs: "2", "dateParse" )
                                    ogc.Function_Name( nArgs: "2", "difference" )
                                    ogc.Function_Name( nArgs: "1", "dimension" )
                                    ogc.Function_Name( nArgs: "2", "disjoint" )
                                    ogc.Function_Name( nArgs: "2", "distance" )
                                    ogc.Function_Name( nArgs: "1", "double2bool" )
                                    ogc.Function_Name( nArgs: "1", "endAngle" )
                                    ogc.Function_Name( nArgs: "1", "endPoint" )
                                    ogc.Function_Name( nArgs: "1", "env" )
                                    ogc.Function_Name( nArgs: "1", "envelope" )
                                    ogc.Function_Name( nArgs: "2", "EqualInterval" )
                                    ogc.Function_Name( nArgs: "2", "equalsExact" )
                                    ogc.Function_Name( nArgs: "3", "equalsExactTolerance" )
                                    ogc.Function_Name( nArgs: "2", "equalTo" )
                                    ogc.Function_Name( nArgs: "1", "exp" )
                                    ogc.Function_Name( nArgs: "1", "exteriorRing" )
                                    ogc.Function_Name( nArgs: "1", "floor" )
                                    ogc.Function_Name( nArgs: "1", "geometryType" )
                                    ogc.Function_Name( nArgs: "1", "geomFromWKT" )
                                    ogc.Function_Name( nArgs: "1", "geomLength" )
                                    ogc.Function_Name( nArgs: "2", "getGeometryN" )
                                    ogc.Function_Name( nArgs: "1", "getX" )
                                    ogc.Function_Name( nArgs: "1", "getY" )
                                    ogc.Function_Name( nArgs: "1", "getz" )
                                    ogc.Function_Name( nArgs: "2", "greaterEqualThan" )
                                    ogc.Function_Name( nArgs: "2", "greaterThan" )
                                    ogc.Function_Name( nArgs: "0", "id" )
                                    ogc.Function_Name( nArgs: "2", "IEEEremainder" )
                                    ogc.Function_Name( nArgs: "3", "if_then_else" )
                                    ogc.Function_Name( nArgs: "11", "in10" )
                                    ogc.Function_Name( nArgs: "3", "in2" )
                                    ogc.Function_Name( nArgs: "4", "in3" )
                                    ogc.Function_Name( nArgs: "5", "in4" )
                                    ogc.Function_Name( nArgs: "6", "in5" )
                                    ogc.Function_Name( nArgs: "7", "in6" )
                                    ogc.Function_Name( nArgs: "8", "in7" )
                                    ogc.Function_Name( nArgs: "9", "in8" )
                                    ogc.Function_Name( nArgs: "10", "in9" )
                                    ogc.Function_Name( nArgs: "1", "int2bbool" )
                                    ogc.Function_Name( nArgs: "1", "int2ddouble" )
                                    ogc.Function_Name( nArgs: "1", "interiorPoint" )
                                    ogc.Function_Name( nArgs: "2", "interiorRingN" )
                                    ogc.Function_Name( nArgs: "3", "Interpolate" )
                                    ogc.Function_Name( nArgs: "2", "intersection" )
                                    ogc.Function_Name( nArgs: "2", "intersects" )
                                    ogc.Function_Name( nArgs: "1", "isClosed" )
                                    ogc.Function_Name( nArgs: "1", "isEmpty" )
                                    ogc.Function_Name( nArgs: "2", "isLike" )
                                    ogc.Function_Name( nArgs: "1", "isNull" )
                                    ogc.Function_Name( nArgs: "2", "isometric" )
                                    ogc.Function_Name( nArgs: "1", "isRing" )
                                    ogc.Function_Name( nArgs: "1", "isSimple" )
                                    ogc.Function_Name( nArgs: "1", "isValid" )
                                    ogc.Function_Name( nArgs: "3", "isWithinDistance" )
                                    ogc.Function_Name( nArgs: "2", "Jenks" )
                                    ogc.Function_Name( nArgs: "1", "length" )
                                    ogc.Function_Name( nArgs: "2", "lessEqualThan" )
                                    ogc.Function_Name( nArgs: "2", "lessThan" )
                                    ogc.Function_Name( nArgs: "1", "log" )
                                    ogc.Function_Name( nArgs: "2", "max" )
                                    ogc.Function_Name( nArgs: "2", "max_2" )
                                    ogc.Function_Name( nArgs: "2", "max_3" )
                                    ogc.Function_Name( nArgs: "2", "max_4" )
                                    ogc.Function_Name( nArgs: "2", "min" )
                                    ogc.Function_Name( nArgs: "2", "min_2" )
                                    ogc.Function_Name( nArgs: "2", "min_3" )
                                    ogc.Function_Name( nArgs: "2", "min_4" )
                                    ogc.Function_Name( nArgs: "1", "mincircle" )
                                    ogc.Function_Name( nArgs: "1", "minimumdiameter" )
                                    ogc.Function_Name( nArgs: "1", "minrectangle" )
                                    ogc.Function_Name( nArgs: "2", "modulo" )
                                    ogc.Function_Name( nArgs: "1", "not" )
                                    ogc.Function_Name( nArgs: "2", "notEqualTo" )
                                    ogc.Function_Name( nArgs: "2", "numberFormat" )
                                    ogc.Function_Name( nArgs: "5", "numberFormat2" )
                                    ogc.Function_Name( nArgs: "1", "numGeometries" )
                                    ogc.Function_Name( nArgs: "1", "numInteriorRing" )
                                    ogc.Function_Name( nArgs: "1", "numPoints" )
                                    ogc.Function_Name( nArgs: "1", "octagonalenvelope" )
                                    ogc.Function_Name( nArgs: "3", "offset" )
                                    ogc.Function_Name( nArgs: "2", "overlaps" )
                                    ogc.Function_Name( nArgs: "1", "parseBoolean" )
                                    ogc.Function_Name( nArgs: "1", "parseDouble" )
                                    ogc.Function_Name( nArgs: "1", "parseInt" )
                                    ogc.Function_Name( nArgs: "1", "parseLong" )
                                    ogc.Function_Name( nArgs: "0", "pi" )
                                    ogc.Function_Name( nArgs: "2", "pointN" )
                                    ogc.Function_Name( nArgs: "2", "pow" )
                                    ogc.Function_Name( nArgs: "1", "property" )
                                    ogc.Function_Name( nArgs: "1", "PropertyExists" )
                                    ogc.Function_Name( nArgs: "2", "Quantile" )
                                    ogc.Function_Name( nArgs: "0", "random" )
                                    ogc.Function_Name( nArgs: "5", "Recode" )
                                    ogc.Function_Name( nArgs: "2", "relate" )
                                    ogc.Function_Name( nArgs: "3", "relatePattern" )
                                    ogc.Function_Name( nArgs: "1", "rint" )
                                    ogc.Function_Name( nArgs: "1", "round" )
                                    ogc.Function_Name( nArgs: "1", "round_2" )
                                    ogc.Function_Name( nArgs: "1", "roundDouble" )
                                    ogc.Function_Name( nArgs: "2", "setCRS" )
                                    ogc.Function_Name( nArgs: "1", "sin" )
                                    ogc.Function_Name( nArgs: "1", "sqrt" )
                                    ogc.Function_Name( nArgs: "2", "StandardDeviation" )
                                    ogc.Function_Name( nArgs: "1", "startAngle" )
                                    ogc.Function_Name( nArgs: "1", "startPoint" )
                                    ogc.Function_Name( nArgs: "1", "strCapitalize" )
                                    ogc.Function_Name( nArgs: "2", "strConcat" )
                                    ogc.Function_Name( nArgs: "2", "strEndsWith" )
                                    ogc.Function_Name( nArgs: "2", "strEqualsIgnoreCase" )
                                    ogc.Function_Name( nArgs: "2", "strIndexOf" )
                                    ogc.Function_Name( nArgs: "2", "strLastIndexOf" )
                                    ogc.Function_Name( nArgs: "1", "strLength" )
                                    ogc.Function_Name( nArgs: "2", "strMatches" )
                                    ogc.Function_Name( nArgs: "3", "strPosition" )
                                    ogc.Function_Name( nArgs: "4", "strReplace" )
                                    ogc.Function_Name( nArgs: "2", "strStartsWith" )
                                    ogc.Function_Name( nArgs: "3", "strSubstring" )
                                    ogc.Function_Name( nArgs: "2", "strSubstringStart" )
                                    ogc.Function_Name( nArgs: "1", "strToLowerCase" )
                                    ogc.Function_Name( nArgs: "1", "strToUpperCase" )
                                    ogc.Function_Name( nArgs: "1", "strTrim" )
                                    ogc.Function_Name( nArgs: "3", "strTrim2" )
                                    ogc.Function_Name( nArgs: "2", "symDifference" )
                                    ogc.Function_Name( nArgs: "1", "tan" )
                                    ogc.Function_Name( nArgs: "1", "toDegrees" )
                                    ogc.Function_Name( nArgs: "1", "toRadians" )
                                    ogc.Function_Name( nArgs: "2", "touches" )
                                    ogc.Function_Name( nArgs: "1", "toWKT" )
                                    ogc.Function_Name( nArgs: "2", "union" )
                                    ogc.Function_Name( nArgs: "2", "UniqueInterval" )
                                    ogc.Function_Name( nArgs: "1", "vertices" )
                                    ogc.Function_Name( nArgs: "2", "within" )
                                }
                            }
                        }
                    }
                }
            }
        }

        def z = new StreamingMarkupBuilder( encoding: 'UTF-8' ).bind( y )

        results = z?.toString()
        contentType = 'application/xml'

        return [results, contentType]
    }

    def describeFeatureType(def wfsRequest)
    {
        def results, contentType


        def workspace = getWorkspace()

        def layers
        if (wfsRequest.typeName)
        {
            layers = [wfsRequest.typeName]
        }
        else
        {
            layers = layerNames
        }
        def outputFormat = wfsRequest.outputFormat?wfsRequest.outputFormat:""
        outputFormat = outputFormat.toLowerCase()
        if (outputFormat)
        {
            if (!outputFormat.contains("xml")&&
                !outputFormat.contains("gml"))
            {
                throw new Exception("WFS describeFeatureType, outputFormat not supported ${wfsRequest?.outputFormat} only xml gml supported")
            }
        }
        def y = {
            mkp.xmlDeclaration()
            mkp.declareNamespace( gml: "http://www.opengis.net/gml" )
            mkp.declareNamespace( omar: "http://omar.ossim.org" )
            mkp.declareNamespace( xsd: "http://www.w3.org/2001/XMLSchema" )

            xsd.schema(
                    elementFormDefault: "qualified",
                    targetNamespace: "http://omar.ossim.org"
            ) {
                xsd.'import'( namespace: "http://www.opengis.net/gml",
                        schemaLocation: "http://schemas.opengis.net/gml/2.1.2/feature.xsd" )
                for(def layerName in layers)
                {
                    def layer = workspace[layerName]
                    if(layer)
                    {
                        xsd.complexType( name: "${ layer.name }Type" ) {
                            xsd.complexContent {
                                xsd.extension( base: "gml:AbstractFeatureType" ) {
                                    xsd.sequence {
                                        for ( def field in layer.schema.fields )
                                        {
                                            def descr = layer.schema.featureType.getDescriptor( field.name )
                                            xsd.element(
                                                    maxOccurs: "${ descr.maxOccurs }",
                                                    minOccurs: "${ descr.minOccurs }",
                                                    name: "${ field.name }",
                                                    nillable: "${ descr.nillable }",
                                                    type: "${ typeMappings.get( field.typ, field.typ ) }" )
                                        }
                                    }
                                }
                            }
                        }
                        xsd.element( name: layer.name, substitutionGroup: "gml:_Feature", type: "omar:${ layer.name }Type" )
                    }
                    else
                    {
                        throw new Exception("Layer name not found ${layerName}")
                    }
                }
            }
        }

        //def z = y
        def z = new StreamingMarkupBuilder( encoding: 'UTF-8' ).bind( y )

        results = z?.toString()
        contentType = 'application/xml'
        workspace.close()

        return [results, contentType]

    }

    def getFeature(def wfsRequest)
    {
        def results, contentType

        //if ( wfsRequest.resultType?.toLowerCase() == "hits" )
        //{
        //  results = outputGML( wfsRequest )
        //  contentType = 'text/xml; subtype=gml/2.1.2'
        //}
        //else
        //{
        switch ( wfsRequest?.outputFormat?.toUpperCase() ?: "" )
        {
            case "SHP":
                contentType = "application/octet-stream"
                break;
            case "GML2":
                results = outputGML( wfsRequest )
                contentType = 'text/xml; subtype=gml/2.1.2'
                break
            case "CSV":
                results = outputCSV( wfsRequest )
                contentType = 'text/csv'
                break
            case "KML":
                results = outputKML( wfsRequest )
                contentType = 'application/vnd.google-earth.kml+xml'
                break
            case "KMLQUERY":
                results = outputKMLQuery( wfsRequest )
                contentType = 'application/vnd.google-earth.kml+xml'
                break
            case "JSON":
            case "GEOJSON":
                results = outputJSON( wfsRequest )
                contentType = 'application/json'
                break
            default:
                    results = outputGML( wfsRequest )
                    contentType = 'text/xml; subtype=gml/2.1.2'
        }
        //}

        return [results, contentType]
    }
    private getTagLib(){
        grailsApplication.mainContext.getBean("org.codehaus.groovy.grails.plugins.web.taglib.ApplicationTagLib")

    }
    private String outputGML(def wfsRequest)
    {
        def results
        def describeFeatureTypeURL = grailsLinkGenerator.link( base: grailsApplication.config.omar.serverURL, absolute: true,
                controller: 'wfs', params: [service: 'WFS', version: '1.0.0', request: 'DescribeFeatureType',
                typeName: "${ wfsRequest.typeName }"] )

        def filterParams = [
                filter: wfsRequest?.filter ?: Filter.PASS,
                max: wfsRequest.maxFeatures ?: -1,
                start: wfsRequest?.offset ?: -1
        ]
        if ( wfsRequest.sortBy )
        {
            filterParams.sort = wfsRequest.convertSortByToArray();
        }
        def filter
        try
        {
//        println "BEFORE"
            filter = new Filter( filterParams.filter )
//        println "AFTER"
        }
        catch ( e )
        {
            e.printStackTrace()
        }
        def y

        if ( wfsRequest.resultType?.toLowerCase() == "hits" )
        {
            def workspace = getWorkspace()
            def layer = workspace[wfsRequest?.typeName]
            def count = layer.count( filter );
            // println "COUNT = ${count}";
            def timestamp = new DateTime( DateTimeZone.UTC );
            y = {
                mkp.xmlDeclaration()
                mkp.declareNamespace( wfs: "http://www.opengis.net/wfs" )
                mkp.declareNamespace( omar: "http://omar.ossim.org" )
                mkp.declareNamespace( gml: "http://www.opengis.net/gml" )
                mkp.declareNamespace( xsi: "http://www.w3.org/2001/XMLSchema-instance" )

                wfs.FeatureCollection(
                        xmlns: 'http://www.opengis.net/wfs',
                        'xsi:schemaLocation': "http://omar.ossim.org ${ describeFeatureTypeURL } http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.0.0/WFS-basic.xsd",
                        'numberOfFeatures': "${count}",
                        "timestamp": "${timestamp}"
                )
            }
        }
        else
        {
            y = {
                def workspace = getWorkspace()
                def layer = workspace[wfsRequest?.typeName]

                //println wfsRequest?.filter

//      xxx.each { println it }


                def cursor = layer.getCursor( filterParams )

                mkp.xmlDeclaration()
                mkp.declareNamespace( wfs: "http://www.opengis.net/wfs" )
                mkp.declareNamespace( omar: "http://omar.ossim.org" )
                mkp.declareNamespace( gml: "http://www.opengis.net/gml" )
                mkp.declareNamespace( xsi: "http://www.w3.org/2001/XMLSchema-instance" )

                wfs.FeatureCollection(
                        xmlns: 'http://www.opengis.net/wfs',
                        'xsi:schemaLocation': "http://omar.ossim.org ${ describeFeatureTypeURL } http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.0.0/WFS-basic.xsd"
                ) {
                    gml.boundedBy {
                        gml.'null'( "unknown" )
                    }

                    while ( cursor?.hasNext() )
                    {
                        def feature = cursor.next()
                        def featureId = feature.id
                        //println feature

                        gml.featureMember {
                            omar."${ wfsRequest?.typeName }"( fid: featureId ) {

                                for ( def attribute in feature.attributes )
                                {
                                    if ( attribute?.value != null )
                                    {

                                        if ( attribute.key == "ground_geom" )
                                        {
                                            omar.ground_geom {

                                                /*
                                                gml.Polygon( srsName: "http://www.opengis.net/gml/srs/epsg.xml#4326" ) {
                                                  gml.outerBoundaryIs {
                                                    gml.LinearRing {
                                                      gml.coordinates( 'xmlns:gml': "http://www.opengis.net/gml", decimal: ".", cs: ",", ts: "", """
                                                    -122.56492547,38.02596313 -122.1092658,38.02339409 -122.11359067,37.66295699
                                                    -122.56703818,37.66549309 -122.56492547,38.02596313""" )
                                                    }
                                                  }
                                                }
                                                */

                                                def geom = new XmlSlurper( false, false ).parseText( feature.ground_geom.gml2 as String )

                                                geom.@srsName = 'http://www.opengis.net/gml/srs/epsg.xml#4326'

                                                mkp.yield( geom )

                                            }
                                        }
                                        else
                                        {
                                            //println "${ attribute.key }: ${ typeMappings[feature.schema.field( attribute.key ).typ] }"

                                            switch ( attribute.key )
                                            {
                                                case "other_tags_xml":
                                                case "tie_point_set":
                                                    omar."${ attribute.key }" {
                                                        mkp.yieldUnescaped( "<![CDATA[${ attribute.value }]]>" )
                                                    }
                                                    break
                                                default:
                                                    switch ( typeMappings[feature.schema.field( attribute.key ).typ] )
                                                    {
                                                        case "xsd:dateTime":
                                                            //println attribute.value?.format( "yyyy-MM-dd'T'hh:mm:ss.SSS" )
                                                            omar."${ attribute.key }"( attribute.value?.format( "yyyy-MM-dd'T'hh:mm:ss.SSS" ) )
                                                            break
                                                        default:
                                                            omar."${ attribute.key }"( attribute.value )
                                                    }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                cursor?.close()
                workspace?.close()
            }
        }

        def z = new StreamingMarkupBuilder( encoding: 'UTF-8' ).bind( y )

        results = z?.toString()

        return results
    }
    private String createKmlDescription( def wfsRequest,
                                         def feature)
    {
        def flashDirRoot = grailsApplication.config.videoStreaming.flashDirRoot
        def flashUrlRoot = grailsApplication.config.videoStreaming.flashUrlRoot
        def fields
        def labels
        def formatters
        def typeName = wfsRequest?.typeName.toLowerCase();
        def thumbnail
        def url
        def tagLibBean = getTagLib();
        def omarServerUrl = grailsApplication.config.omar.serverURL
       // def flvUrl
       // def flashPlayerUrl = tagLibBean.createLinkTo(dir: "js", file: "player.swf", base: "${grailsApplication.config.omar.serverURL}", absolute: true)
        def mpegFile = feature["filename"] as File
        def flvFile = "${flashDirRoot}/${mpegFile.name}.flv" as File
        if ( typeName == "raster_entry" )
        {
            fields = grailsApplication.config.export.rasterEntry.fields
            labels = grailsApplication.config.export.rasterEntry.labels
            formatters = grailsApplication.config.export.rasterEntry.formatters
            url = tagLibBean.createLink( absolute: true, base: omarServerUrl,
                    controller: "mapView", params: [layers: feature["index_id"]] )

            thumbnail = tagLibBean.createLink( absolute: true, base: omarServerUrl,
                    controller: "thumbnail", action: "show", id: feature["id"],
                    params: [size: 128, projectionType: 'imagespace'] )
        }
        else if ( typeName == "video_data_set" )
        {
            fields     = grailsApplication.config.export.videoDataSet.fields
            labels     = grailsApplication.config.export.videoDataSet.labels
            formatters = grailsApplication.config.export.videoDataSet.formatters
            url = tagLibBean.createLink(absolute: true, base: omarServerUrl,
                    controller: "videoStreaming",
                    action: "show",
                    id: feature['index_id'])
            thumbnail = tagLibBean.createLink(absolute: true,
                                              base: omarServerUrl,
                                              controller: "thumbnail",
                                              action: "frame",
                                              id: feature['id'],
                                              params: [size: 128])
           // flvUrl = new URL("${flashUrlRoot}/${flvFile.name}")
        }
        def description = new StringWriter()
        description << "<table border='1'>"
        description << "<tr>"
        description << "<th align='right'>Thumbnail:</th>"
        description << "<td><a href='${url}'><img src='${thumbnail}'/></a></td></tr>"


        (0..fields.size()-1).each{idx->
            def field = fields[idx];
            def label = labels[idx];
            def value

            def adjustedField = field.replaceAll( "[a-z][A-Z]", { v -> "${v[0]}_${v[1].toLowerCase()}" } )

            if ( formatters && formatters[field] )
            {
                value = formatters[field].call( feature[adjustedField] )
            }
            else
            {
                value = feature[adjustedField]
            }

            if (field == "filename")
            {
                description << "<tr>"
                description << "<th align='right'>${label}:</th>"
                description << "<td><a href='${url}'>${(value as File).name}</a></td></tr>"
            }
            else
            {
                description << "<tr>"
                description << "<th align='right'>${label}:</th>"
                description << "<td>${value}</td></tr>"
            }
        }
        def searchUrl = tagLibBean.createLink(absolute: true, base: "${grailsApplication.config.omar.serverURL}",controller: "federation", action: "search")
        description << "<tr>"
        description << "<th align='right'>Search:</th>"
        description << "<td><a href='${searchUrl}'>Find More Data</a></td></tr>"

        def logoUrl = "${grailsApplication.config.omar.serverURL}/images/omarLogo.png"
               description << "<tfoot><tr><td colspan='2'><a href='${grailsApplication.config.omar.serverURL}'><img src='${logoUrl}'/></a></td></tr></tfoot>"
        description << "</table>"

        description.buffer
    }
    private String outputKML(def wfsRequest)
    {
        def tagLibBean = getTagLib()
        def wmsParams = [:]
        def caseInsensitiveParams = new CaseInsensitiveMap( wfsRequest.properties )
        def pushPin = tagLibBean.resource(absolute: true, base: "${grailsApplication.config.omar.serverURL}", plugin: "omar-common-ui", dir: "images/google", file: "red-pushpin.png")
        SimpleDateFormat isdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        SimpleDateFormat osdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        caseInsensitiveParams.format = "image/png"
        caseInsensitiveParams.version = "1.1.0"
        caseInsensitiveParams.transparent = "TRUE"
        caseInsensitiveParams.request = "GetMap"
        caseInsensitiveParams.service = "WMS"
        caseInsensitiveParams.srs = "EPSG:4326"
        caseInsensitiveParams.stretch_mode = "linear_auto_min_max"
        caseInsensitiveParams.stretch_mode_region = "viewport"
        caseInsensitiveParams.bands = "default"

        caseInsensitiveParams.each { wmsParams.put( it.key.toLowerCase(), it.value )}
        wmsParams = wmsParams.subMap( wmsPersistParams )
        wmsParams.remove( "elevation" )
        wmsParams.remove( "time" )
        wmsParams?.remove( "bbox" )
        wmsParams?.remove( "width" )
        wmsParams?.remove( "height" )
        wmsParams.remove( "action" )
        wmsParams.remove( "controller" )

        def workspace = getWorkspace()
        def layer = workspace[wfsRequest?.typeName]
        def filterParams = [
                filter: wfsRequest?.filter ?: Filter.PASS,
                max: wfsRequest.maxFeatures ?: -1,
                start: wfsRequest?.offset ?: -1,
        ]
        if ( wfsRequest.sortBy )
        {
            filterParams.sort = wfsRequest.convertSortByToArray();
        }
        def bbox
        def sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        // def cursor = layer.getCursor( filterParams )
        def kmlBuilder = new StreamingMarkupBuilder();
        def kmlwriter = new StringWriter()
        kmlwriter << """<?xml version='1.0'?><kml xmlns='http://earth.google.com/kml/2.1'>"""
        kmlwriter << "<Document>"

        def cursor = layer.getCursor( filterParams )
        while ( cursor?.hasNext() )
        {
            def feature = cursor.next();
            def description = createKmlDescription(wfsRequest, feature);
            def bounds = feature["ground_geom"].bounds
            def groundCenterLon = ( bounds?.minX + bounds?.maxX ) * 0.5;
            def groundCenterLat = ( bounds?.minY + bounds?.maxY ) * 0.5;
            def renderedHtml = description

            if(wfsRequest?.typeName?.toLowerCase() == "raster_entry")
            {
                kmlwriter << "<name>OMAR Rasters</name>"

                def acquisition = ( feature["acquisition_date"] ) ? sdf.format( feature["acquisition_date"] ) : null
                // wmsParams?.layers = feature["index_id"]
                def mpp = feature["gsdy"]//rasterEntry.getMetersPerPixel()
                // calculate a crude metric for putting an image that almost fits within the google viewport
                //
                def defaultRange = mpp * Math.sqrt( ( feature["width"] ** 2 ) + ( feature["height"] ** 2 ) );
                if ( defaultRange < 1 )
                {
                    defaultRange = 15000
                }

                kmlwriter << "<GroundOverlay><name>${feature['title']?:(feature['filename'] as File).name}</name><Snippet/><description><![CDATA[${renderedHtml}]]>}</description>"
                kmlwriter << "<LookAt><longitude>${groundCenterLon}</longitude><latitude>${groundCenterLat}</latitude><altitude>0.0</altitude><heading>0.0</heading><tilt>0.0</tilt><range>${defaultRange}</range><altitudeMode>clampToGround</altitudeMode></LookAt>"
                kmlwriter << "<open>1</open>"
                kmlwriter << "<visibility>1</visibility>"
                wmsParams.layers=feature['index_id']
                def wmsURL = tagLibBean.createLink(
                        absolute: true, base: "${grailsApplication.config.omar.serverURL}",
                        controller: "ogc", action: "wms", params: wmsParams
                )
                kmlwriter << "<Icon><href><![CDATA[${wmsURL}]]></href>" <<
                        "<viewRefreshMode>onStop</viewRefreshMode><viewRefreshTime>1</viewRefreshTime>" <<
                        "<viewBoundScale>0.85</viewBoundScale>" <<
                        "<viewFormat><![CDATA[BBOX=[bboxWest],[bboxSouth],[bboxEast],[bboxNorth]&width=[horizPixels]&height=[vertPixels]]]></viewFormat></Icon>"

                kmlwriter <<"<LatLonBox>"
                //  if (bbox)
                //  {
                //
                //  }
                //  else
                //  {
                kmlwriter << "<north>${bounds?.maxY}</north><south>${bounds?.minY}</south>"
                kmlwriter << "<east>${bounds?.maxX}</east><west>${bounds?.minX}</west>"
                //  }
                kmlwriter <<"</LatLonBox>"
                if ( acquisition )
                {
                    kmlwriter << "<TimeStamp><when>${acquisition}</when></TimeStamp>"
                }
                kmlwriter << "</GroundOverlay>"
            }
            else
            {
                def flashbasename = "${FilenameUtils.getBaseName(feature['filename'])}.flv"
                def createFlvUrl = tagLibBean.createLink(absolute: true, base: "${grailsApplication.config.omar.serverURL}",controller: "videoStreaming", action: "show", id: feature['index_id'])
              //  def descriptionText = ""
              //  def logoUrl = "${grailsApplication.config.omar.serverURL}/images/omarLogo.png"
              //  def thumbnailUrl = tagLibBean.createLink(absolute: true, base: "${grailsApplication.config.omar.serverURL}", controller: "thumbnail", action: "frame", id: feature['id'], params: [size: 128])
                kmlwriter << "<name>OMAR Videos</name>"
                def styleBuilder = new StreamingMarkupBuilder().bind{
                    Style("id": "sh_red") {
                        LineStyle() {
                            color("ffOOOOff")
                        }
                        PolyStyle {
                            color("7f00005f")
                        }
                        IconStyle {
                            color("ff00007f")
                            scale("1.0")
                            Icon() {
                               href("${pushPin}")
                            }
                            hotspot("x": "20", "y": "2", "xunits": "pixels", "yunits": "pixels")
                        }
                    }
                    Style("id": "sn_red") {
                        LineStyle() {
                            color("ff00007f")
                        }
                        PolyStyle {
                            color("3f00001f")
                        }
                        IconStyle {
                            color("ff00007f")
                            scale("1.0")
                            Icon() {
                                href("${pushPin}")
                            }
                            hotspot("x": "20", "y": "2", "xunits": "pixels", "yunits": "pixels")
                        }
                    }
                    StyleMap("id": "red") {
                        Pair() {
                            key("normal")
                            styleUrl("#sn_red")
                        }
                        Pair() {
                            key("highlight")
                            styleUrl("#sh_red")
                        }
                    }
                }
                def point = null
                def polygons = []
                def kmlPoly = ""
                feature['ground_geom'].each() {geom ->
                    // for now until we have a utility to get access to all polgons we will assume multi
                    // geom and each is a poly
                    //
                    (0..geom.getNumGeometries() - 1).each() {geomIdx ->
                        def poly = geom.getGeometryN(geomIdx) as geoscript.geom.Polygon
                        if ( poly )
                        {
                            kmlPoly = ""
                            def ring = poly.getExteriorRing();
                            def coordinates = ring.getCoordinates();
                            if ( coordinates.size() > 0 )
                            {
                                (0..coordinates.size() - 1).each() {coordIdx ->
                                    kmlPoly = "${kmlPoly} ${coordinates[coordIdx].x},${coordinates[coordIdx].y}"
                                    if ( !point )
                                    {
                                        point = "${coordinates[coordIdx].x},${coordinates[coordIdx].y}"
                                    }
                                }
                            }
                            polygons.add(kmlPoly)
                        }
                    }
                }
                def multiGeometryBuilder = new StreamingMarkupBuilder().bind{
                    MultiGeometry() {
                        polygons.each { polygon ->
                            Polygon() {
                                tessellate("1")
                               // altitudeMode("relativeToGround")
                                altitudeMode("clampToGround")
                                outerBoundaryIs() {
                                    LinearRing() {
                                        coordinates("${polygon}")
                                    }
                                }
                            }
                        }
                        Point() {
                            altitudeMode("clampToGround")

                            //altitudeMode("relativeToGround")
                            coordinates("${point}")
                        }
                    } // END MultiGeometry()
                }
                kmlwriter << styleBuilder.toString()

                kmlwriter << "<Placemark><styleUrl>#red</styleUrl>"
                kmlwriter << "<name>${flashbasename}</name>"
                kmlwriter << "<description><![CDATA[${description}]]></description>"
                kmlwriter << "<Snippet><![CDATA[<a href='${createFlvUrl}'>CLICK TO PLAY</a>]]></Snippet>"
                kmlwriter << multiGeometryBuilder.toString()

                if ( feature['start_date'] )
                {
                    kmlwriter << "<Timestamp>"
                    kmlwriter << "<when>${osdf.format(new Date(isdf.parse(feature['start_date'] as String) as String))}</when>"
                    kmlwriter << "</Timestamp>"
                }

                kmlwriter << "</Placemark>"
            }
        }
        kmlwriter << "</Document></kml>"
        cursor?.close()
        workspace?.close()

        kmlwriter.buffer
    }
    private def outputKMLQuery( def wfsRequest )
    {
        def caseInsensitiveParams = new CaseInsensitiveMap();
        wfsRequest.properties.each { caseInsensitiveParams.put( it.key.toLowerCase(),it.value)}
        def filter = caseInsensitiveParams.filter?:""
        def bbox
        if (!filter.contains("BBOX("))
        {
            if (!filter)
            {
                filter = "BBOX(ground_geom,[bboxWest],[bboxSouth],[bboxEast],[bboxNorth])"
            }
            else
            {
                if (filter.endsWith(")"))
                {
                    filter = "${filter}AND(BBOX(ground_geom,[bboxWest],[bboxSouth],[bboxEast],[bboxNorth]))"
                }
                else
                {
                    filter = "(${filter})AND(BBOX(ground_geom,[bboxWest],[bboxSouth],[bboxEast],[bboxNorth]))"
                }
            }
        }
        else
        {
            def bboxString = filter.find("BBOX\\(.*\\)");
            if (bboxString)
            {
                def splitBbox = bboxString.split(",");
                if (splitBbox.size() == 5)
                {
                    def stripEnding = splitBbox[4].trim()
                    stripEnding = stripEnding.substring(0, stripEnding.indexOf(')')-1)
                    bbox = [minx: splitBbox[1].toDouble(),
                            miny: splitBbox[2].toDouble(),
                            maxx: splitBbox[3].toDouble(),
                            maxy: stripEnding.toDouble()
                         ]
                }
            }
        }
        //println filter
        /*
        if (filter.contains("BBOX("))
        {
            filter = filter.replaceAll("BBOX\\(.*\\)", "BBOX(ground_geom,[bboxWest],[bboxSouth],[bboxEast],[bboxNorth])")
        }
        else
        {
            if (!filter)
            {
                filter = "BBOX(ground_geom,[bboxWest],[bboxSouth],[bboxEast],[bboxNorth])"
            }
            else
            {
                if (filter.endsWith(")"))
                {
                    filter = "${filter}AND(BBOX(ground_geom,[bboxWest],[bboxSouth],[bboxEast],[bboxNorth]))"
                }
                else
                {
                    filter = "(${filter})AND(BBOX(ground_geom,[bboxWest],[bboxSouth],[bboxEast],[bboxNorth]))"
                }
            }
        }
        */
        caseInsensitiveParams.remove("filter");
        caseInsensitiveParams.remove("class");
        def tagLibBean = getTagLib()
        caseInsensitiveParams.outputFormat = "kml"

        def kmlQueryUrl = tagLibBean.createLink( absolute: true, base: "${grailsApplication.config.omar.serverURL}",
                controller: "wfs", action: "index", params: caseInsensitiveParams )
        def kmlwriter = new StringWriter()

        kmlwriter << """<?xml version='1.0'?><kml xmlns='http://earth.google.com/kml/2.1'>"""
       // kmlwriter << "<open>1</open>"
       // if (bbox)
       // {
       //     def groundCenterLon = (bbox.minx+bbox.maxx)*0.5
       //     def groundCenterLat = (bbox.miny+bbox.maxy)*0.5
       //     def defaultRange    = 15000;
        //    kmlwriter << "<LookAt><longitude>${groundCenterLon}</longitude><latitude>${groundCenterLat}</latitude><altitude>0.0</altitude><heading>0.0</heading><tilt>0.0</tilt><range>${defaultRange}</range><altitudeMode>clampToGround</altitudeMode></LookAt>"
        //}
        kmlwriter <<"<NetworkLink>"
        kmlwriter <<"<name>KML Query</name>"
        kmlwriter << "<Link>" <<
                "<href><![CDATA[${kmlQueryUrl}]]></href>" <<
                "<httpQuery>googleClientVersion=[clientVersion]</httpQuery>"<<
                "<viewFormat>filter=${filter}</viewFormat>"<<
                "<viewRefreshMode>onRequest</viewRefreshMode>"
        kmlwriter << "</Link></NetworkLink></kml>"
        String kmlText = kmlwriter.buffer
        return kmlText
    }

    private def outputCSV(def wfsRequest)
    {
        def fields
        def labels
        def formatters
        def typeName = wfsRequest?.typeName.toLowerCase();
        if ( typeName == "raster_entry" )
        {
            fields = grailsApplication.config.export.rasterEntry.fields
            labels = grailsApplication.config.export.rasterEntry.labels
            formatters = grailsApplication.config.export.rasterEntry.formatters
        }
        else if ( typeName == "video_data_set" )
        {
            fields = grailsApplication.config.export.videoDataSet.fields
            labels = grailsApplication.config.export.videoDataSet.labels
            formatters = grailsApplication.config.export.videoDataSet.formatters
        }

        def workspace = getWorkspace()
        def layer = workspace[wfsRequest?.typeName]
        def filter = [
                filter: wfsRequest?.filter ?: Filter.PASS,
                //sort: ""// [["<COLUMN NAME>","ASC|DESC"]]
        ]
        def filterParams = [
                filter: wfsRequest?.filter ?: Filter.PASS,
                max: wfsRequest.maxFeatures ?: -1,
                start: wfsRequest?.offset ?: -1,
                //sort: [["<COLUMN NAME>","ASC|DESC"]]
        ]
        if ( wfsRequest.sortBy )
        {
            filterParams.sort = wfsRequest.convertSortByToArray();
        }

        def stringBuffer = new StringWriter()
        def csvWriter = new CSVWriter( stringBuffer )

        csvWriter.writeNext( labels as String[] )

        def cursor = layer.getCursor( filterParams )
        while ( cursor?.hasNext() )
        {
            def feature = cursor.next();
            def data = []
            for ( field in fields )
            {
                def adjustedField = field.replaceAll( "[a-z][A-Z]", { v -> "${v[0]}_${v[1].toLowerCase()}" } )

                if ( formatters && formatters[field] )
                {
                    data << formatters[field].call( feature[adjustedField] )
                }
                else
                {
                    data << feature[adjustedField]
                }
            }

            csvWriter.writeNext( data as String[] )

        }
        csvWriter.close()
        cursor?.close()
        workspace?.close()

        return stringBuffer.toString();
    }

    private def outputJSON(def wfsRequest)
    {
        def results
        def workspace = getWorkspace()
        def layer = workspace[wfsRequest?.typeName]
        def filter = [
                filter: wfsRequest?.filter ?: Filter.PASS,
                //sort: ""// [["<COLUMN NAME>","ASC|DESC"]]
        ]
        def filterParams = [
                filter: wfsRequest?.filter ?: Filter.PASS,
                max: wfsRequest.maxFeatures ?: -1,
                start: wfsRequest?.offset ?: -1,
                //sort: [["<COLUMN NAME>","ASC|DESC"]]
        ]
        if ( wfsRequest.sortBy )
        {

            // filterParams.sort =null//[["TITLE".toUpperCase(),"DESC"]]
            filterParams.sort = wfsRequest.convertSortByToArray();//wfsRequest.sortBy.substring()//JSON.parse( wfsRequest.sort );

            //println filterParams
        }
        try
        {
            filter = new Filter( filterParams.filter )
        }
        catch ( e )
        {
            e.printStackTrace()
        }

        if ( wfsRequest.resultType?.toLowerCase() == "hits" )
        {
            def count = layer.count( filter );
            def timestamp = new DateTime( DateTimeZone.UTC );
            results = "${[numberOfFeatures: count, timestamp: timestamp] as JSON}"
        }
        else
        {
            def writer = new GeoJSONWriter()
            def cursor = layer.getCursor( filterParams );
            def newLayer = new Layer( cursor.col )

            results = writer.write( newLayer )
            cursor?.close()
        }
        workspace?.close()
        return results
    }


    private def getWorkspace(def flag = true)
    {
        def workspace = null

        if ( flag )
        {
            def jdbcParams = grailsApplication.config.dataSource

            def dbParams = [
                    dbtype: "postgis",           //must be postgis
                    user: jdbcParams.username,   //the user to connect with
                    passwd: jdbcParams.password, //the password of the user.
                    schema: "public",
                    'Expose primary keys': true
            ]

            def pattern1 = "jdbc:(.*)://(.*):(.*)/(.*)"
            def pattern2 = "jdbc:(.*)://(.*)/(.*)"
            def pattern3 = "jdbc:(.*):(.*)"

            switch ( jdbcParams.url )
            {
                case ~pattern1:
                    def matcher = ( jdbcParams.url ) =~ pattern1
                    dbParams['host'] = matcher[0][2]
                    dbParams['port'] = matcher[0][3]
                    dbParams['database'] = matcher[0][4]
                    break
                case ~pattern2:
                    def matcher = ( jdbcParams.url ) =~ pattern2
                    dbParams['host'] = matcher[0][2]
                    dbParams['port'] = "5432"
                    dbParams['database'] = matcher[0][3]
                    break
                case ~pattern3:
                    def matcher = ( jdbcParams.url ) =~ pattern3
                    dbParams['host'] = "localhost"
                    dbParams['port'] = "5432"
                    dbParams['database'] = matcher[0][2]
                    break
            }

            def dataStore = new PostgisNGDataStoreFactory().createDataStore( dbParams )

            workspace = new Database( dataStore )
        }
        else
        {
            def dbParams = [
                    'Data Source': dataSourceUnproxied,
                    'Expose primary keys': true
            ]

            def dataStore = new PostgisNGDataStoreFactory().createDataStore( dbParams )

            workspace = new Database( dataStore )
        }

        return workspace
    }
}
