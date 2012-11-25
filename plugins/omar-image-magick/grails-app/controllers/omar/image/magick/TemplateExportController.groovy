package omar.image.magick

import static groovyx.gpars.dataflow.Dataflow.task
import groovyx.gpars.dataflow.DataflowVariable

class TemplateExportController
{
	def footerGeneratorService
	def footerGradientGeneratorService
	def grailsApplication
	def headerGeneratorService
	def headerGradientGeneratorService
	def imageDownloadService
	def northArrowGeneratorService
	def templateExportService

	def index()
	{
		def acquisitionDate = params.acquisitionDate
		def centerGeo = params.centerGeo
		def countryCode = params.countryCode
		def imageId = params.imageId
		def imageUrl = params.imageURL
		def markers = params.markers?.split(",") ?: ["null"]
		def northArrowAngle = params.northArrowAngle
		def securityClassification = grailsApplication.config.security[grailsApplication.config.security.level].description

		render(
			view: "templateExport.gsp",
			model:
			[
				acquisitionDate: acquisitionDate,
				centerGeo: centerGeo,
				countryCode: countryCode,
				imageId: imageId,
				imageURL: imageUrl,
				markers: markers,
				northArrowAngle: northArrowAngle,
				securityClassification: securityClassification
			]
		)
	}

	def export()
	{
		def country = params.country
		def footerAcquisitionDateText = params.footerAcquisitionDateText
		def footerAcquisitionDateTextColor = params.footerAcquisitionDateTextColor
		def footerLocationText = params.footerLocationText
		def footerLocationTextColor = params.footerLocationTextColor
		def footerSecurityClassificationText = params.footerSecurityClassificationText
		def footerSecurityClassificationTextColor = params.footerSecurityClassificationTextColor
		def gradientColorBottom = params.gradientColorBottom
		def gradientColorTop = params.gradientColorTop
		def headerDescriptionText = params.headerDescriptionText
		def headerDescriptionTextColor = params.headerDescriptionTextColor
		def headerTitleText = params.headerTitleText
		def headerTitleTextColor = params.headerTitleTextColor
		def headerSecurityClassificationText = params.headerSecurityClassificationText
		def headerSecurityClassificationTextColor = params.headerSecurityClassificationTextColor
		def imageFile = params.imageUrl
		def imageHeight = params.imageHeight
		def imageWidth = params.imageWidth
		def includeOverviewMap = params.includeOverviewMap
		def logo = params.logo
    		def markerLocations = params.markers?.split(",") ?: ["null"]
		def northAngle = params.northArrowAngle
		def northArrowColor = params.northArrowColor
		def northArrowBackgroundColor = params.northArrowBackgroundColor
		def northArrowSize = params.northArrowSize

		final def footerFilename = new DataflowVariable()
		final def headerFilename = new DataflowVariable()
		final def imageFilename = new DataflowVariable()
		final def northArrowFilename = new DataflowVariable()

		task { imageFilename << imageDownloadService.serviceMethod(imageFile, markerLocations) }
		task { northArrowFilename << northArrowGeneratorService.serviceMethod(northAngle, northArrowBackgroundColor, northArrowColor, northArrowSize) }
		task { headerFilename << headerGeneratorService.serviceMethod(gradientColorBottom, gradientColorTop, headerDescriptionText, headerDescriptionTextColor, headerSecurityClassificationText, headerSecurityClassificationTextColor, headerTitleText, headerTitleTextColor, imageHeight, imageWidth, logo) }
		task { footerFilename << footerGeneratorService.serviceMethod(footerAcquisitionDateText, footerAcquisitionDateTextColor, footerLocationText, footerLocationTextColor, footerSecurityClassificationText, footerSecurityClassificationTextColor, gradientColorBottom, gradientColorTop, imageHeight, imageWidth) }	
		
		def finishedProductFilename = templateExportService.serviceMethod(country, footerFilename.val, headerFilename.val, imageFilename.val, imageHeight, includeOverviewMap, northArrowFilename.val)
		def file = new File( "${finishedProductFilename}" )
		if ( file.exists() )
		{
			response.setContentType( "application/octet-stream" )
			response.setHeader( "Content-disposition", "attachment; filename=${file.name}" )
			response.outputStream << file.bytes

			def removeImageFile = "rm ${file}"
			def removeImageFileProc = removeImageFile.execute()
			removeImageFileProc.waitFor()
		}
	}

	def footerGradientGenerator()
	{
		def gradientColorTop = params.gradientColorTop
		def gradientColorBottom = params.gradientColorBottom
		def gradientHeight = params.gradientHeight

		def fileName = footerGradientGeneratorService.serviceMethod( gradientColorTop, gradientColorBottom, gradientHeight )

		def file = new File( "${fileName}" )
		if ( file.exists() )
		{
			response.setHeader( "Content-length", "" + file.bytes.length )
			response.contentType = "image/png"
			response.outputStream << file.bytes
			response.outputStream.flush()

			def removeImageFile = "rm ${file}"
			def removeImageFileProc = removeImageFile.execute()
			removeImageFileProc.waitFor()
		}
	}

	def headerGradientGenerator()
	{
                def gradientColorTop = params.gradientColorTop
                def gradientColorBottom = params.gradientColorBottom
                def gradientHeight = params.gradientHeight

                def fileName = headerGradientGeneratorService.serviceMethod( gradientColorTop, gradientColorBottom, gradientHeight )

                def file = new File( "${fileName}" )
                if ( file.exists() )
                {
                        response.setHeader( "Content-length", "" + file.bytes.length )
                        response.contentType = "image/png"
                        response.outputStream << file.bytes
                        response.outputStream.flush()

                        def removeImageFile = "rm ${file}"
                        def removeImageFileProc = removeImageFile.execute()
                        removeImageFileProc.waitFor()
                }
        }

	def northArrowGenerator()
	{
		def northAngle = params.northAngle
		def northArrowBackgroundColor = params.northArrowBackgroundColor
		def northArrowColor = params.northArrowColor
		def northArrowSize = params.northArrowSize

		def fileName = northArrowGeneratorService.serviceMethod(northAngle, northArrowBackgroundColor, northArrowColor, northArrowSize)

		def file = new File( "${fileName}" )
		if ( file.exists() )
		{
			response.setHeader( "Content-length", "" + file.bytes.length )
			response.contentType = "image/png"
			response.outputStream << file.bytes
			response.outputStream.flush()
			
			def removeImageFile = "rm ${file}"
			def removeImageFileProc = removeImageFile.execute()
			removeImageFileProc.waitFor()
		}
	}

}
