<div id="surveyModal" class="modal fade" tabindex="-1" role="dialog">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">${grailsApplication.config.survey.header}
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </h5>
            </div>
            <div class="modal-body">
                <div id="alertContent">
                    ${grailsApplication.config.survey.html.encodeAsRaw()}
                </div>
                <br/>
                <!-- dialog buttons -->
                <div class="modal-footer">
                    <button type="button" class="btn secondary" data-dismiss="modal"><g:message code="show.close" /></button>
                    <button type="button" class="btn btn-primary" id="survey-ok"><g:message code="show.ok" /></button>
                </div>
            </div>
        </div><!-- /.modal-content -->
    </div><!-- /.modal-dialog -->
</div>


<!-- external survey -->
<script type="text/javascript" defer="true">
    $(function(){
        var surveySeen = false
        var cookies = document.cookie.split(';')
        for (let c in cookies) {
            var idx = cookies[c].indexOf("${grailsApplication.config.survey.url}=")
            if (idx == 0 || idx == 1) {
                surveySeen = true
            }
        }

        var localStorageEnabled = true
        try {
            if (localStorage.getItem("${grailsApplication.config.survey.url}")) {
                surveySeen = true
            }
        } catch (e) {
            localStorageEnabled = false
        }

        // do not show if cannot track
        if (!navigator.cookieEnabled && !localStorageEnabled) {
            surveySeen = true
        }

        if (!surveySeen) {
            var expiry = new Date(new Date().getTime() + ${grailsApplication.config.survey.cookieAge}*86400000).toUTCString()
            document.cookie = "${grailsApplication.config.survey.url}=true; expires=" + expiry + "; path=/;"
            localStorageEnabled && localStorage.setItem("${grailsApplication.config.survey.url}", 'true');
            $('#surveyModal').modal('show');
        }

        $('#survey-ok').click(function() {
            window.open("${grailsApplication.config.survey.url}", "_blank")
            $('#surveyModal').modal('hide');
        })
    })
</script>
