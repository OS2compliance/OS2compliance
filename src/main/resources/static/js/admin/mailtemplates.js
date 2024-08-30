let currentEditingMessage = null;

function getFormData(oForm) {
    var formData = new FormData(oForm);
    var indexed_array = {};

    formData.forEach(function(value, key) {
        if (key == "message" && currentEditingMessage != null) {
            indexed_array[key] = currentEditingMessage;
        } else {
            indexed_array[key] = value;
        }
    });

    return indexed_array;
}

function save(payload, tryEmail) {
    postData(restUrl + "?tryEmail=" + tryEmail, payload).then((response) => {
        if (!response.ok) {
            throw new Error(`${response.status} ${response.statusText}`);
        }

        if (tryEmail) {
            toastService.info("Test email sendt");
        } else {
            toastService.info("Gemt!");
        }
    }).catch(error => {toastService.error(error)});
}

function pageLoaded() {
    let editors = document.querySelectorAll(`textarea[name=message]`)
    for (let i=0; i<editors.length; ++i) {
        ClassicEditor.create(editors[i])
            .then(editor => {
                            editor.editing.view.document.on('blur', () => {
                                currentEditingMessage = editor.getData();
                            });
                        })
            .catch( error => {
                toastService.error(error);
            });
    }

    document.getElementById('templateDropdown').addEventListener('change', function() {
        // hide all
        document.querySelectorAll('.templateForm').forEach(function(element) {
            element.style.display = 'none';
        });

        // reset
        currentEditingMessage = null;

        // show chosen
        var selectedTemplate = document.getElementById('template' + this.value);
        if (selectedTemplate) {
            selectedTemplate.style.display = 'block';
        }
    });

    document.querySelectorAll('.buttonSubmit').forEach(function(button) {
        button.addEventListener('click', function() {
            var oForm = document.getElementById('template' + document.getElementById('templateDropdown').value);
            var data = getFormData(oForm);

            save(data, false);
        });
    });

    document.querySelectorAll('.buttonTest').forEach(function(button) {
        button.addEventListener('click', function() {
            var oForm = document.getElementById('template' + document.getElementById('templateDropdown').value);
            var data = getFormData(oForm);

            save(data, true);
        });
    });

    document.querySelectorAll('.checkboxFancy').forEach(function(checkbox) {
        checkbox.addEventListener('change', function() {
            var oForm = document.getElementById('template' + document.getElementById('templateDropdown').value);
            var checkboxValue = oForm.querySelector('.checkboxFancy').checked;

            oForm.querySelector("input[name=enabled]").value = checkboxValue;
        });
    });

    // display currently chosen template
    var initialTemplate = document.getElementById('template' + document.getElementById('templateDropdown').value);
    if (initialTemplate) {
        initialTemplate.style.display = 'block';
    }

}
