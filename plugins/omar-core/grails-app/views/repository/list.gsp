<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <meta name="layout" content="generatedViews"/>
  <title>OMAR: Repository List</title>
</head>
<body>
<content tag="content">
  <div class="nav">
    <span class="menuButton"><g:link class="home" uri="/">OMAR™ Home</g:link></span>
    <sec:ifAllGranted roles="ROLE_ADMIN">
      <span class="menuButton"><g:link class="create" action="create">Create Repository</g:link></span>
    </sec:ifAllGranted>
  </div>
  <div class="body">
    <h1>OMAR: Repository List</h1>
    <g:if test="${flash.message}">
      <div class="message">${flash.message}</div>
    </g:if>
    <div class="list">
      <table>
        <thead>
        <tr>
          <g:sortableColumn property="id" title="Id"/>
          <g:sortableColumn property="baseDir" title="Base Dir"/>
          <g:sortableColumn property="scanStartDate" title="Scan Start Date"/>
          <g:sortableColumn property="scanEndDate" title="Scan End Date"/>
        </tr>
        </thead>
        <tbody>
        <g:each in="${repositoryList}" status="i" var="repository">
          <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
            <td><g:link action="show" id="${repository.id}">${fieldValue(bean: repository, field: 'id')}</g:link></td>
            <td>${fieldValue(bean: repository, field: 'baseDir')}</td>
            <td>${fieldValue(bean: repository, field: 'scanStartDate')}</td>
            <td>${fieldValue(bean: repository, field: 'scanEndDate')}</td>
          </tr>
        </g:each>
        </tbody>
      </table>
    </div>
    <div class="paginateButtons">
      <g:paginate total="${repositoryList.totalCount}"/>
    </div>
  </div>
</content>
</body>
</html>
