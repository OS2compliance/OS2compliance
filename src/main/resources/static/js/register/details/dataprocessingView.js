export default function RegisterDataprocessingService() {
    this.init = function () {
    }

    this.setDataprocessingEditState = function (editable) {
        const rootElement = document.getElementById('dataprocessingForm');
        document.getElementById('saveDataProcessingBtn').hidden = !editable;
        document.getElementById('cancelDataProcessingBtn').hidden = !editable;
        document.getElementById('editDataProcessingBtn').hidden = editable;
        rootElement.querySelectorAll('.editField').forEach(elem => {
            elem.disabled = !editable;
            if (elem.tagName === "A") {
                elem.hidden = editable;
                elem.nextElementSibling.hidden = !editable;
            }
        });
        if (!editable) {
            rootElement.reset();
        }
        editModeCategoryInformationEditable(editable);
    }

}