function refreshSchema() {
    fetch(`/assets/dpia/schema/fragment`)
        .then(response => response.text()
            .then(data => {
                let placeholder = document.getElementById('schemaPlaceholder');
                placeholder.innerHTML = data;

                let editors = document.querySelectorAll(`textarea[name=instructions]`);
                for (let i = 0; i < editors.length; ++i) {
                    ClassicEditor.create(editors[i])
                        .then(editor => {
                            editor.editing.view.document.on('blur', () => {
                                var textarea = editors[i];
                                setFieldOnQuestion(textarea.dataset.questionid, "instructions", editor.getData());
                            });
                        })
                        .catch(error => {
                            toastService.error(error);
                        });
                }

                // Bind foldable section events after content is loaded
                const sectionRows = document.querySelectorAll('.dpiaSchemaSectionTr');
                for (let i = 0; i < sectionRows.length; i++) {
                    var row = sectionRows[i];
                    var sectionId = row.dataset.sectionid;
                    handleSectionRow(sectionId);
                    row.addEventListener('click', sectionRowClicked, false);
                }

                // Bind change event for checkboxes
                const checkboxes = document.querySelectorAll('input[type="checkbox"][id^="sectionCheckbox"]');
                for (let i = 0; i < checkboxes.length; i++) {
                    checkboxes[i].addEventListener('click', (event) => event.stopPropagation()); // do not fold out/in section
                    checkboxes[i].addEventListener('change', checkboxChanged);
                }

                // Bind click event section sort btns
                const sectionSortHigherBtns = document.querySelectorAll('.sortSectionHigherBtn');
                for (let i = 0; i < sectionSortHigherBtns.length; i++) {
                    var btn = sectionSortHigherBtns[i];
                    btn.addEventListener('click', sortSectionHigher, false);
                }
                const sectionSortLowerBtns = document.querySelectorAll('.sortSectionLowerBtn');
                for (let i = 0; i < sectionSortLowerBtns.length; i++) {
                    var btn = sectionSortLowerBtns[i];
                    btn.addEventListener('click', sortSectionLower, false);
                }

                // Bind click event question sort btns
                const questionSortHigherBtns = document.querySelectorAll('.sortQuestionHigherBtn');
                for (let i = 0; i < questionSortHigherBtns.length; i++) {
                    var btn = questionSortHigherBtns[i];
                    btn.addEventListener('click', sortQuestionHigher, false);
                }
                const questionSortLowerBtns = document.querySelectorAll('.sortQuestionLowerBtn');
                for (let i = 0; i < questionSortLowerBtns.length; i++) {
                    var btn = questionSortLowerBtns[i];
                    btn.addEventListener('click', sortQuestionLower, false);
                }
            }))
        .catch(error => toastService.error(error));
}

function sortSectionHigher(event) {
    event.stopPropagation();  // do not fold out/in section
    var sectionId = parseInt(this.dataset.sectionid);
    fetch(`/rest/assets/dpia/schema/section/${sectionId}/up`,
            {method: "POST", headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': token}})
            .then(response => refreshSchema())
            .catch(error => toastService.error(error));
}

function sortSectionLower(event) {
    event.stopPropagation();  // do not fold out/in section
    var sectionId = parseInt(this.dataset.sectionid);
    fetch(`/rest/assets/dpia/schema/section/${sectionId}/down`,
                {method: "POST", headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': token}})
                .then(response => refreshSchema())
                .catch(error => toastService.error(error));
}

function sortQuestionHigher(event) {
    var questionId = parseInt(this.dataset.questionid);
    fetch(`/rest/assets/dpia/schema/question/${questionId}/up`,
            {method: "POST", headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': token}})
            .then(response => refreshSchema())
            .catch(error => toastService.error(error));
}

function sortQuestionLower(event) {
    var questionId = parseInt(this.dataset.questionid);
    fetch(`/rest/assets/dpia/schema/question/${questionId}/down`,
                {method: "POST", headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': token}})
                .then(response => refreshSchema())
                .catch(error => toastService.error(error));
}

function setFieldOnQuestion(questionId, fieldName, value) {
    var data = {
        "id": questionId,
        "fieldName": fieldName,
        "value": value
    };
    putData(`/rest/assets/dpia/schema/question/setfield`, data)
        .then(defaultResponseHandler)
        .catch(defaultErrorHandler);
}

function setFieldOnSection(sectionId, fieldName, value) {
    var data = {
        "id": sectionId,
        "fieldName": fieldName,
        "value": value
    };
    putData(`/rest/assets/dpia/schema/section/setfield`, data)
        .then(defaultResponseHandler)
        .catch(defaultErrorHandler);
}

function sectionRowClicked() {
    var sectionId = this.dataset.sectionid;
    handleSectionRow(sectionId);
}

function checkboxChanged(event) {
    const sectionId = event.target.dataset.sectionid;
    const checked = event.target.checked;
    console.log(`Section ${sectionId} has been ${checked ? 'excluded' : 'included'}.`);
    setFieldOnSection(sectionId, "hasOptedOut", checked);
}

function handleSectionRow(sectionId) {
    var show = false;
    var icon = document.getElementById("sectionIcon" + sectionId);
    const belongingRows = document.querySelectorAll('.sectionRow' + sectionId);

    for (var i = 0; i < belongingRows.length; i++) {
        var elem = belongingRows[i];
        if (elem.hidden) {
            if (i == 0) {
                show = true;
            }
            elem.hidden = false;
        } else {
            elem.hidden = true;
        }
    }

    if (show) {
        icon.classList.add("pli-arrow-up");
        icon.classList.remove("pli-arrow-down");
    } else {
        icon.classList.remove("pli-arrow-up");
        icon.classList.add("pli-arrow-down");
    }
}

function pageLoaded() {
    refreshSchema();  // refreshSchema now also handles event binding
}
