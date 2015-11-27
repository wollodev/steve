<%@ include file="../00-header.jsp" %>
<script type="text/javascript">
    $(document).ready(function() {
        <%@ include file="../snippets/datePicker-past.js" %>
    });
</script>
<spring:hasBindErrors name="userForm">
    <div class="error">
        Error while trying to update a user:
        <ul>
            <c:forEach var="error" items="${errors.allErrors}">
                <li>${error.defaultMessage}</li>
            </c:forEach>
        </ul>
    </div>
</spring:hasBindErrors>
<div class="content"><div>
    <section><span>User Details</span></section>
    <form:form action="/steve/manager/users/update" modelAttribute="userForm">

        <form:hidden path="userPk" readonly="true"/>
        <table class="userInput">
            <thead><tr><th>Profile</th><th></th></thead>
            <tr><td>First name:</td><td><form:input path="firstName"/></td></tr>
            <tr><td>Last name:</td><td><form:input path="lastName"/></td></tr>
            <tr><td>Birthday:</td><td><form:input path="birthDay" cssClass="datePicker"/></td></tr>
            <tr>
                <td>Sex:</td>
                <td><form:select path="sex">
                    <form:options items="${sex}" itemLabel="value"/>
                </form:select>
                </td>
            </tr>
            <tr><td>Phone:</td><td><form:input path="phone"/></td></tr>
            <tr><td>E-mail:</td><td><form:input path="eMail"/></td></tr>
            <tr><td>Additional Note:</td><td><form:input path="note"/></td></tr>
        </table>

        <form:hidden path="address.addressPk" readonly="true"/>
        <table class="userInput">
            <thead><tr><th>Address</th><th></th></thead>
            <tr><td>Street and House Number:</td><td><form:input path="address.streetAndHouseNumber"/></td></tr>
            <tr><td>Zip code:</td><td><form:input path="address.zipCode"/></td></tr>
            <tr><td>City:</td><td><form:input path="address.city"/></td></tr>
            <tr><td>Country:</td><td><form:input path="address.country"/></td></tr>
        </table>

        <table class="userInput">
            <thead><tr><th>OCPP</th><th></th></thead>
            <tbody>
            <tr>
                <td>Ocpp ID Tag:</td>
                <td><form:select path="ocppIdTag" items="${idTagList}" /></td>
            </tr>
            <tr><td></td>
                <td id="add_space">
                    <input type="submit" name="update" value="Update">
                    <input type="submit" name="backToOverview" value="Back to Overview">
                </td></tr>
            </tbody>
        </table>


    </form:form>
</div></div>
<%@ include file="../00-footer.jsp" %>