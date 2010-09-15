<%@ page import="org.ossim.omar.RasterDataSet" %>
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <meta name="layout" content="main"/>
  <title>OMAR: Create Raster File</title>
</head>
<body>
<content tag="content">
  <div class="nav">
    <span class="menuButton"><g:link class="home" uri="/">OMAR™ Home</g:link></span>
    <span class="menuButton"><g:link class="list" action="list">Raster File List</g:link></span>
  </div>
  <div class="body">
    <h1>Create Raster File</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <g:hasErrors bean="${rasterFile}">
      <div class="errors">
        <g:renderErrors bean="${rasterFile}" as="list"/>
      </div>
    </g:hasErrors>
    <g:form action="save" method="post">
      <div class="dialog">
        <table>
          <tbody>
          <tr class="prop">
            <td valign="top" class="name">
              <label for="name">Name:</label>
            </td>
            <td valign="top" class="value ${hasErrors(bean: rasterFile, field: 'name', 'errors')}">
              <input type="text" id="name" name="name" value="${fieldValue(bean: rasterFile, field: 'name')}"/>
            </td>
          </tr>
          <tr class="prop">
            <td valign="top" class="name">
              <label for="type">Type:</label>
            </td>
            <td valign="top" class="value ${hasErrors(bean: rasterFile, field: 'type', 'errors')}">
              <input type="text" id="type" name="type" value="${fieldValue(bean: rasterFile, field: 'type')}"/>
            </td>
          </tr>
          <tr class="prop">
            <td valign="top" class="name">
              <label for="rasterDataSet">Raster Data Set:</label>
            </td>
            <td valign="top" class="value ${hasErrors(bean: rasterFile, field: 'rasterDataSet', 'errors')}">
              <g:select optionKey="id" from="${RasterDataSet.list()}" name="rasterDataSet.id" value="${rasterFile?.rasterDataSet?.id}"></g:select>
            </td>
          </tr>
          </tbody>
        </table>
      </div>
      <div class="buttons">
        <span class="button"><input class="save" type="submit" value="Create"/></span>
      </div>
    </g:form>
  </div>
</content>
</body>
</html>