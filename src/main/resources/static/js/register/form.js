

let createRegisterService = new CreateRegisterService();
let editRegisterService = new EditRegisterService();
document.addEventListener("DOMContentLoaded", function() {
    createRegisterService.init();
    editRegisterService.init();
});

function CreateRegisterService() {
    this.init = function() {}

    this.show = function() {
        fetch(formUrl)
            .then(response => response.text()
                .then(data => {
                    let dialog = document.getElementById('createRegisterDialog');
                    dialog.innerHTML = data;
                    this.onLoaded();
                    const editRegisterModal = new bootstrap.Modal(dialog);
                    editRegisterModal.show();
                    initFormValidationForForm('createForm');
                }))
            .catch(error => {toastService.error(error); console.log(error)});
    }

    this.onLoaded = function() {
        choiceService.initUserSelect('createFormUserSelect', false);
        initOUSelect('createFormOuSelect', false);
    }

}

function EditRegisterService() {
    this.init = function() {}

    this.show = function(registerId) {
        fetch(`${formUrl}?id=${registerId}`)
            .then(response => response.text()
                .then(data => {
                    let dialog = document.getElementById('editRegisterDialog');
                    dialog.innerHTML = data;
                    this.onLoaded();
                    const createRegisterModal = new bootstrap.Modal(dialog);
                    createRegisterModal.show();
                    initFormValidationForForm('editForm');
                }))
            .catch(error => {toastService.error(error); console.log(error);});
    }

    this.onLoaded = function() {
        choiceService.initUserSelect('editFormUserSelect', false);
        initOUSelect('editFormOuSelect', false);
    }
}
