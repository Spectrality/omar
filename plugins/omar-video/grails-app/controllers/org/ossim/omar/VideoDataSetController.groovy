package org.ossim.omar

import org.springframework.beans.factory.InitializingBean
import groovy.xml.StreamingMarkupBuilder

import grails.converters.JSON
import grails.converters.deep.XML

class VideoDataSetController implements InitializingBean
{

  public static final List tagHeaderList = []
  public static final List tagNameList = []

  def thumbnailSize = 128

  def baseWMS
  def dataWMS

  def index = { redirect(action: list, params: params) }

  def authenticateService
  def videoDataSetSearchService

  // the delete, save and update actions only accept POST requests
  def static allowedMethods = [delete: 'POST', save: 'POST', update: 'POST']

  def list = {
    if ( !params.max )
    params.max = 10

    def videoDataSetList = null

    if ( params.repositoryId )
    {
      def repository = Repository.get(params.repositoryId)

      videoDataSetList = VideoDataSet.createCriteria().list(params) {
        eq("repository", repository)
      }
    }
    else
    {
      videoDataSetList = VideoDataSet.createCriteria().list(params) {}
    }

    //[videoDataSetList: videoDataSetList]
    withFormat {
      html { [videoDataSetList: videoDataSetList] }
      xml { render videoDataSetList as XML }
      json { render videoDataSetList as JSON }
    }
  }

  def show = {
    def videoDataSet = VideoDataSet.get(params.id)

    if ( !videoDataSet )
    {
      flash.message = "VideoDataSet not found with id ${params.id}"
      redirect(action: list)
    }
    else
    {
      withFormat {
        html { [videoDataSet: videoDataSet] }
        xml { render videoDataSet as XML }
        json { render videoDataSet as JSON }
      }
    }
  }

  def delete = {
    def videoDataSet = VideoDataSet.get(params.id)
    if ( videoDataSet )
    {
      videoDataSet.delete()
      flash.message = "VideoDataSet ${params.id} deleted"
      redirect(action: list)
    }
    else
    {
      flash.message = "VideoDataSet not found with id ${params.id}"
      redirect(action: list)
    }
  }

  def edit = {
    def videoDataSet = VideoDataSet.get(params.id)

    if ( !videoDataSet )
    {
      flash.message = "VideoDataSet not found with id ${params.id}"
      redirect(action: list)
    }
    else
    {
      return [videoDataSet: videoDataSet]
    }
  }

  def update = {
    def videoDataSet = VideoDataSet.get(params.id)
    if ( videoDataSet )
    {
      videoDataSet.properties = params
      if ( !videoDataSet.hasErrors() && videoDataSet.save() )
      {
        flash.message = "VideoDataSet ${params.id} updated"
        redirect(action: show, id: videoDataSet.id)
      }
      else
      {
        render(view: 'edit', model: [videoDataSet: videoDataSet])
      }
    }
    else
    {
      flash.message = "VideoDataSet not found with id ${params.id}"
      redirect(action: edit, id: params.id)
    }
  }

  def create = {
    def videoDataSet = new VideoDataSet()
    videoDataSet.properties = params
    return ['videoDataSet': videoDataSet]
  }

  def save = {
    def videoDataSet = new VideoDataSet(params)
    if ( !videoDataSet.hasErrors() && videoDataSet.save() )
    {
      flash.message = "VideoDataSet ${videoDataSet.id} created"
      redirect(action: show, id: videoDataSet.id)
    }
    else
    {
      render(view: 'create', model: [videoDataSet: videoDataSet])
    }
  }

  def search = {

    //println "=== search start ==="

    if ( !params.max )
    {
      params.max = 10;
    }

    //println "\nparams: ${params?.sort { it.key }}"

    def queryParams = initVideoDataSetQuery(params)

    //println "\nqueryParams: ${queryParams?.toMap()?.sort { it.key } }"

    if ( request.method == 'POST' )
    {
      if ( !params.max || !(params.max =~ /\d+$/) || (params.max as Integer) > 100 )
      {
        params.max = 10
      }

      params.order = 'desc'
      params.sort = 'startDate'

      //println "queryParams: ${queryParams}"

      def user = authenticateService.principal().username
      def starttime = System.currentTimeMillis()

      def videoDataSets = videoDataSetSearchService.runQuery(queryParams, params)
      def totalCount = videoDataSetSearchService.getCount(queryParams)

      def videoFiles = []

      if ( videoDataSets )
      {
        videoFiles = VideoFile.createCriteria().list {
          eq("type", "main")
          inList("videoDataSet", videoDataSets)
        }
      }

      def endtime = System.currentTimeMillis()

      def logData = [
          TYPE: "video_search",
          START: new Date(starttime),
          END: new Date(endtime),
          ELAPSE_TIME_MILLIS: endtime - starttime,
          USER: user,
          PARAMS: params
      ]

      log.info(logData)

      //println logData

      //println "=== search end ==="

      chain(action: "results",
              model: [videoDataSets: videoDataSets, totalCount: totalCount, videoFiles: videoFiles],
              params: params
      )
    }
    else
    {
      //println "=== search end ==="

      return [queryParams: queryParams, baseWMS: baseWMS, dataWMS: dataWMS]
    }
  }

  private def initVideoDataSetQuery(Map params)
  {
    def queryParams = new VideoDataSetQuery()

    bindData(queryParams, params)

    queryParams.startDate = DateUtil.initializeDate("startDate", params)
    queryParams.endDate = DateUtil.initializeDate("endDate", params)

//    println "params: ${params}"
//    println "startDate: ${queryParams.startDate}"
//    println "endDate: ${queryParams.endDate}"

    return queryParams
  }

  def results = {

    //println "=== results start ==="

    def starttime = System.currentTimeMillis()

    if ( !params.max || !(params.max =~ /\d+$/) || (params.max as Integer) > 100 )
    {
      params.max = 10
    }

    def videoDataSets = null
    def totalCount = null
    def videoFiles = null

    def queryParams = initVideoDataSetQuery(params)

    if ( chainModel )
    {
      videoDataSets = chainModel.videoDataSets
      totalCount = chainModel.totalCount
      videoFiles = chainModel.videoFiles
    }
    else
    {
      videoDataSets = videoDataSetSearchService.runQuery(queryParams, params)
      totalCount = videoDataSetSearchService.getCount(queryParams)

      if ( videoDataSets )
      {
        videoFiles = VideoFile.createCriteria().list {
          eq("type", "main")
          inList("videoDataSet", videoDataSets)
        }
      }

      def endtime = System.currentTimeMillis()
      def user = authenticateService.principal()?.username

      def logData = [
          TYPE: "video_search",
          START: new Date(starttime),
          END: new Date(endtime),
          ELAPSE_TIME_MILLIS: endtime - starttime,
          USER: user,
          PARAMS: params
      ]

      //println "\nparams: ${params?.sort { it.key }}"
      //println "\nqueryParams: ${queryParams?.toMap()?.sort { it.key } }"

      log.info(logData)

      //println logData
    }

    //println "=== results end ==="

    if(!session.videoDataSetResultCurrentTab&&("${session.videoDataSetResultCurrentTab}"!="0"))
    {
      session["videoDataSetResultCurrentTab"] = "0"
    }
    render(view: 'results', model: [
        videoDataSets: videoDataSets,
        videoFiles: videoFiles,
        totalCount: totalCount,
        tagNameList: tagNameList,
        tagHeaderList: tagHeaderList,
        queryParams: queryParams,
        sessionAction:"updateSession",
        sessionController:"session",
        videoDataSetResultCurrentTab:session["videoDataSetResultCurrentTab"]
    ])

  }

  def kmlnetworklink = {
    def kmlbuilder = new StreamingMarkupBuilder()

    kmlbuilder.encoding = "UTF-8"


    params.remove("_action_kmlnetworklink")

    params.dateSort = "false"

    def serviceAddress = createLink(absolute: true, controller: "kmlQuery", action: "getVideosKml", params: params)

    def kmlnode = {
      mkp.xmlDeclaration()
      kml("xmlns": "http://earth.google.com/kml/2.1") {
        Folder() {
          name("Videos")
          visibility("1")
          open("1")
          description("")
          NetworkLink() {
            name("Video Query")
            visibility("1")
            open("1")
            description("")
            refreshVisibility("0")
            flyToView("0")
            Link() {
              href() {
                mkp.yieldUnescaped("<![CDATA[${serviceAddress}]]>")
              }
              refreshInterval("2000")
              refreshMode("onRequest")
              refreshTime("200")
            }
          }
        }
      }
    }
    kmlbuilder.bind(kmlnode)
    response.setHeader("Content-disposition", "attachment; filename=singleRequestTopVideos.kml")
    render(contentType: "application/vnd.google-earth.kml+xml", text: kmlbuilder.bind(kmlnode).toString(), encoding: "UTF-8")

  }

  public void afterPropertiesSet()
  {
    baseWMS = grailsApplication.config.wms.base.layers
    dataWMS = grailsApplication.config.wms.data.video
  }

  def listTest = {
    params.max = Math.min(params.max ? params.int('max') : 10, 100)
    params.offset = params.offset ?: 0
    params.sort = params.sort ?: "id"
    params.order = params.order ?: "asc"

    def queryParams = initVideoDataSetQuery(params)

    def initialRequest = g.createLink(action: "query.json", params: queryParams.toMap())
    initialRequest = initialRequest.substring(initialRequest.indexOf('?') + 1)

    def myColumnDefs = [
            [key: 'thumbnail', label: 'Thumbnail', sortable: false, resizeable: true, width: thumbnailSize, formatter: 'thumbnail'],
            [key: 'id', label: 'Id', sortable: true, resizeable: true],
            [key: 'width', label: 'Width', sortable: true, resizeable: true],
            [key: 'height', label: 'Height', sortable: true, resizeable: true],
            [key: 'startDate', label: 'Start Date', sortable: false, resizeable: true],
            [key: 'endDate', label: 'End Date', sortable: false, resizeable: true],
            [key: 'minLon', label: 'Min Lon', sortable: false, resizeable: true],
            [key: 'minLat', label: 'Min Lat', sortable: false, resizeable: true],
            [key: 'maxLon', label: 'Max Lon', sortable: false, resizeable: true],
            [key: 'maxLat', label: 'Max Lat', sortable: false, resizeable: true],
            [key: 'filename', label: 'Filename', sortable: true, resizeable: true]
    ]

    def fields = [
            [key: 'thumbnail'],
            [key: 'id'],
            [key: 'width'],
            [key: 'height'],
            [key: 'startDate'],
            [key: 'endDate'],
            [key: 'minLon'],
            [key: 'minLat'],
            [key: 'maxLon'],
            [key: 'maxLat'],
            [key: 'filename']
    ]

    return [
            initialRequest: initialRequest,
            myColumnDefs: myColumnDefs as JSON,
            fields: fields as JSON
    ]
  }

  def query = {
    params.max = Math.min(params.max ? params.int('max') : 10, 100)
    params.offset = params.offset ?: 0
    params.sort = params.sort ?: "id"
    params.order = params.order ?: "asc"

    def queryParams = initVideoDataSetQuery(params)

    def videoDataSet = videoDataSetSearchService.runQuery(queryParams, params)
    def videoDataSetTotal = videoDataSetSearchService.getCount(queryParams)

    def results = videoDataSet.collect {
      def thumbnailURL = g.createLink(controller: "thumbnail", action: "frame", id: it.id, params: [size: thumbnailSize])
      def thumbnailTarget = g.createLink(controller: "videoStreaming", action: "show", params: [id: it.indexId])
      def startDate = it.startDate.toString()
      def endDate = it.endDate.toString()
      def bounds = it.groundGeom?.bounds

      def records = [
              thumbnail: [url: thumbnailURL, href: thumbnailTarget],
              id: it.id,
              width: it.width,
              height: it.height,
              startDate: startDate,
              endDate: endDate,
              minLon: bounds.minLon,
              minLat: bounds.minLat,
              maxLon: bounds.maxLon,
              maxLat: bounds.maxLat,
              filename: it.mainFile.name
      ]
      return records
    }

    withFormat {
      json {
        def data = [
                totalRecords: videoDataSetTotal,
                results: results
        ]

        render contentType: "application/json", text: data as JSON
      }
      xml {
        def data = [
                totalRecords: videoDataSetTotal,
                results: results
        ]

        render contentType: "application/xml", text: data as XML
      }
    }
  }



























  
}
