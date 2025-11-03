let token = document.getElementsByName("_csrf")[0].getAttribute("content");

document.addEventListener("DOMContentLoaded", async function(event) {
    fetch(formUrl)
        .then(response => response.text()
            .then(data => document.getElementById('formDialog').innerHTML = data))
        .catch(error => toastService.error(error));

    addRelationFormLoaded();

    const { default: initRelatedTagList } = await import("../tags/related-tag-list.js"); // Dynamic import. Should be replaced ASAP
    initRelatedTagList();
});

function addRelationFormLoaded() {
    initAssetRelationSelectPrivate();
    initDocumentRelationSelectPrivate();
    initTaskRelationSelectPrivate();
    initIncidentRelationSelectPrivate();
}

function initIncidentRelationSelectPrivate() {
    const relationsSelect = document.getElementById('IncidentRelationModalrelationsSelect');
    let relationsChoice = initSelect(relationsSelect);
    relationsSelect.addEventListener("search", (event) => {
            choiceService.updateRelationsIncidentsOnly(relationsChoice, event.detail.value);
        },
        false,
    );
    relationsSelect.addEventListener("change", (event) => {
            choiceService.updateRelationsIncidentsOnly(relationsChoice, "");
        },
        false,
    );
}

function initAssetRelationSelectPrivate() {
    const relationsSelect = document.getElementById('AssetRelationModalrelationsSelect');
    let relationsChoice = initSelect(relationsSelect);
    choiceService.updateRelationsAssetsOnly(relationsChoice, "");
    relationsSelect.addEventListener("search",
        function(event) {
            choiceService.updateRelationsAssetsOnly(relationsChoice, event.detail.value);
        },
        false,
    );
    relationsSelect.addEventListener("change",
        function(event) {
            choiceService.updateRelationsAssetsOnly(relationsChoice, "");
        },
        false,
    );
}

function initDocumentRelationSelectPrivate() {
    const relationsSelect = document.getElementById('DocumentRelationModalrelationsSelect');
    let relationsChoice = initSelect(relationsSelect);
    choiceService.updateRelationsDocumentsOnly(relationsChoice, "");
    relationsSelect.addEventListener("search",
        function(event) {
            choiceService.updateRelationsDocumentsOnly(relationsChoice, event.detail.value);
        },
        false,
    );
    relationsSelect.addEventListener("change",
        function(event) {
            choiceService.updateRelationsDocumentsOnly(relationsChoice, "");
        },
        false,
    );
}

function initTaskRelationSelectPrivate() {
    const relationsSelect = document.getElementById('TaskRelationModalrelationsSelect');
    let relationsChoice = initSelect(relationsSelect);
    choiceService.updateRelationsTasksOnly(relationsChoice, "");
    relationsSelect.addEventListener("search",
        function(event) {
            choiceService.updateRelationsTasksOnly(relationsChoice, event.detail.value);
        },
        false,
    );
    relationsSelect.addEventListener("change",
        function(event) {
            choiceService.updateRelationsTasksOnly(relationsChoice, "");
        },
        false,
    );
}
