

function updateDocumentSearch(choices, search) {
    fetch( `/rest/relatable/autocomplete?types=DOCUMENT &search=${search}`)
        .then(response => response.json()
            .then(data => {
                choices.setChoices(data.content.map(reg => {
                    return {
                        id: reg.id,
                        name: truncateString(reg.typeMessage + ": " + reg.name, 60)
                    }
                }), 'id', 'name', true);
            }))
        .catch(error => toastService.error(error));
}

function updateRelSearch(choices, search) {
    fetch( `/rest/relatable/autocomplete?types=STANDARD_SECTION&search=${search}`)
        .then(response => response.json()
            .then(data => {
                choices.setChoices(data.content.map(reg => {
                    return {
                        id: reg.id,
                        name: truncateString(reg.typeMessage + ": " + reg.name, 60)
                    }
                }), 'id', 'name', true);
            }))
        .catch(error => toastService.error(error));
}

function standardsFormLoaded() {
    let editors = document.querySelectorAll(`textarea[name=description]`)
    for (let i=0; i<editors.length; ++i) {
        window.CreateCkEditor(editors[i], editor => {});
    }
    let docSelects = document.querySelectorAll(`select[name=documents]`)
    for (let i = 0; i < docSelects.length; i++) {
        let select = docSelects[i];
        let choice = initSelect(select);
        select.addEventListener("search",
            function(event) {
                updateDocumentSearch(choice, event.detail.value);
            },
            false,
        );
    }
    let relSelects = document.querySelectorAll(`select[name=relations]`)
    for (let i = 0; i < relSelects.length; i++) {
        let select = relSelects[i];
        let choice = initSelect(select);
        select.addEventListener("search",
            function(event) {
                updateRelSearch(choice, event.detail.value);
            },
            false,
        );
    }
}

function setEditState(button, state) {
    const sectionId=button.getAttribute('data-id');
    let form = document.getElementById(`sectionForm${sectionId}`);
    let cancelButton = document.getElementById(`cancelSectionBtn${sectionId}`);
    let editButton = document.getElementById(`editSectionBtn${sectionId}`);
    let saveButton = document.getElementById(`saveSectionBtn${sectionId}`);
    let editorContainer = document.getElementById(`editorContainer${sectionId}`);
    let readDescription = document.getElementById(`descriptionRead${sectionId}`);
    let statusSelect = document.getElementById(`statusSelect${sectionId}`);
    let documentsEdit = document.getElementById(`documentsEdit${sectionId}`);
    let documentsShow = document.getElementById(`documentsShow${sectionId}`);
    let relationsEdit = document.getElementById(`relationsEdit${sectionId}`);
    let relationsShow = document.getElementById(`relationsShow${sectionId}`);

    editorContainer.style = state ? 'display: block' : 'display: none';
    readDescription.style = state ? 'display: none' : 'display: block';
    cancelButton.style = state ? 'display: block' : 'display: none';
    saveButton.style = state ? 'display: block' : 'display: none';
    editButton.style = state ? 'display: none' : 'display: block';
    documentsShow.style = state ? 'display: none' : 'display: block';
    documentsEdit.style = !state ? 'display: none' : 'display: block';
    relationsShow.style = state ? 'display: none' : 'display: block';
    relationsEdit.style = !state ? 'display: none' : 'display: block';
    statusSelect.disabled = !state;
    if (!state) {
        // Cancelled
        form.reset();
    }
}

function submitForm(event, button, state) {
    //stop form from submitting
    event.preventDefault();

    const sectionId = button.getAttribute('data-id');

    //variables for req body
    let relationsSelect = document.getElementById(`relationsSelect${sectionId}`);
    let statusSelect = document.getElementById(`statusSelect${sectionId}`);
    let statusValue = statusSelect.value;  // Single select, can use .value
    let documentsSelect = document.getElementById(`documentsSelect${sectionId}`);
    let editDescript = document.querySelector("#editorContainer" + sectionId + " textarea").value;

    //manually handle submit with fetch
    let form = document.getElementById(`sectionForm${sectionId}`);


    let data = {
        id: sectionId,
        description: editDescript,
        documents: [...documentsSelect.options].map(opt => opt.value),
        relations: [...relationsSelect.options].map(opt => opt.value),
        status: statusValue
    }

    postData(form.getAttribute('action'), data)
        .then(response => {
            if (response.status == 200) {
                setEditState(button, state)

                //update ui relations
                let relationsShow = document.getElementById(`relationsShow${sectionId}`);
                let relationATagsText = [...relationsSelect.options].map(opt => opt.text);
                relationsShow.innerHTML = ""; //clear so we can override
                relationATagsText.forEach(text => {
                    let div = document.createElement('div');
                    let node = document.createElement('a');
                    //set a tags href
                    node.href = window.location.origin + `/standards/section/${sectionId}`;

                    //set inner text for the link
                    let innerText = text.includes(":") ? text.split(':')[1].trim() : text;
                    node.innerText = innerText
                    //put a tag in a div for appearance
                    div.appendChild(node);
                    relationsShow.appendChild(div);
                })
                //update ui documents
                let documentATagsText = [...documentsSelect.options].map(opt => opt.text);
                let documentsShow = document.getElementById(`documentsShow${sectionId}`);
                documentsShow.innerHTML = ""; // Clear so we essentially override
                documentATagsText.forEach(text => {
                    let div = document.createElement('div');
                    let node = document.createElement('a');

                    node.href = `${window.location.origin}/documents/${sectionId}`;

                    // Apply the same logic to the documents part
                    let innerText = text.includes(":") ? text.split(':')[1].trim() : text;

                    node.innerText = innerText;

                    div.appendChild(node);
                    documentsShow.appendChild(div);
                });


                //update ui textbox for descript
                let description = document.getElementById(`descriptionRead${sectionId}`)
                description.innerHTML = editDescript;

                statusSelect.value = statusValue;
            }
            else {
                toastService.error(response.status)
            }
        }).catch(error => toastService.error(error));

}
