var managersChoicesEditSelect = null;
var suppliersChoicesEditSelect = null;

function initDatePickers() {
    const contractDatePicker = MCDatepicker.create({
        el: '#contractDate',
        autoClose: true,
        dateFormat: 'dd/mm-yyyy',
        closeOnBlur: true,
        firstWeekday: 1,
        customWeekDays: ["sø", "ma", "ti", "on", "to", "fr", "lø"],
        customMonths: ["Januar", "Februar", "Marts", "April", "Maj", "Juni", "Juli", "August", "September", "Oktober", "November", "December"],
        customClearBTN: "Ryd",
        customCancelBTN: "Annuller"
    });
    document.querySelector( "#contractDateBtn" ).addEventListener( "click", () => {
        contractDatePicker.open();
    });


    const contractTerminationPicker = MCDatepicker.create({
        el: '#contractTermination',
        autoClose: true,
        dateFormat: 'dd/mm-yyyy',
        closeOnBlur: true,
        firstWeekday: 1,
        customWeekDays: ["sø", "ma", "ti", "on", "to", "fr", "lø"],
        customMonths: ["Januar", "Februar", "Marts", "April", "Maj", "Juni", "Juli", "August", "September", "Oktober", "November", "December"],
        customClearBTN: "Ryd",
        customCancelBTN: "Annuller"
    });
    document.querySelector( "#contractTerminationBtn" ).addEventListener( "click", () => {
        contractTerminationPicker.open();
    });
}

function removeChoiceByUserUuid(arr, uuid) {
  const objWithIdIndex = arr.findIndex((obj) => obj.uuid === uuid);

  if (objWithIdIndex > -1) {
    arr.splice(objWithIdIndex, 1);
  }

  return arr;
}

function removeChoiceByRegisterId(arr, id) {
    const objWithIdIndex = arr.findIndex((obj) => obj.id === id);
    if (objWithIdIndex > -1) {
      arr.splice(objWithIdIndex, 1);
    }

    return arr;
  }

function updateUsers(choices, search) {
    fetch( `/rest/users/autocomplete?search=${search}`)
        .then(response => response.json()
            .then(data => {
                // Remove already selected users from result
                alreadySelected = []
                var sel = choices.passedElement.element;
                for (var i=0, n=sel.options.length;i<n;i++) {
                  if (sel.options[i].value) alreadySelected.push(sel.options[i].value);
                }

                alreadySelected.forEach((uuid) => removeChoiceByUserUuid(data.content, uuid));

                choices.setChoices(data.content.map(e => {
                    return {
                        value: e.uuid,
                        label: `(${e.userId}) ${e.name}`}
                }), 'value', 'label', false); //false to not override already selected choices
            }))
        .catch(error => console.log(error));
}

function updateSuppliers(choices, search) {
    fetch( `/rest/suppliers/autocomplete?search=${search}`)
        .then(response => response.json()
            .then(data => {
                alreadySelected = []
                var sel = choices.passedElement.element;
                for (var i=0, n=sel.options.length;i<n;i++) {
                  if (sel.options[i].value) alreadySelected.push(sel.options[i].value);
                }
                alreadySelected.forEach((id) => removeChoiceByRegisterId(data.content, id));

                choices.setChoices(data.content.map(e => {
                    return {
                        value: e.id,
                        label: `${e.name}`}
                }), 'value', 'label', true); //false to not override already selected choices
            }))
        .catch(error => console.log(error));
}

function initManagersSelect() {
    const userSelect = document.getElementById('managers');
    const userChoices = initSelect(userSelect);
    updateUsers(userChoices, "");
    userSelect.addEventListener("search",
        function(event) {
            updateUsers(userChoices, event.detail.value);
        },
        false,
    );
    return userChoices;
}

function initSuppliersSelect() {
    var supplierSelect = document.getElementById('supplier');
    const supplierChoices = initSelect(supplierSelect);
    updateSuppliers(supplierChoices, "");
    supplierSelect.addEventListener("search",
        function(event) {
            updateSuppliers(supplierChoices, event.detail.value);
        },
        false,
    );
    supplierChoices.setChoiceByValue(supplierId)
    return supplierChoices;
}

function loadViewAndEditForm() {
    initDatePickers();
    managersChoicesEditSelect = initManagersSelect();
    suppliersChoicesEditSelect = initSuppliersSelect();
    editMode(false);
    suppliersChoicesEditSelect.disable();
    managersChoicesEditSelect.disable();
}

function editMode(enabled) {
    const rootElement = document.getElementById('editForm');
    if (enabled) {
        rootElement.querySelectorAll('.editField').forEach(elem => {
          elem.disabled = false;
          if (elem.tagName === "A") {
              elem.hidden = true;
              if (elem.nextElementSibling) {
                 elem.nextElementSibling.hidden = false;
              }
          }

          if (elem.classList.contains("datepicker")) {
            elem.parentElement.hidden = false; // show the datepicker
            if (elem.parentElement.nextElementSibling) {
                elem.parentElement.nextElementSibling.hidden = true; // hide the "ingen" textfield
            }
          }
        });
        if(!isKitos){
            managersChoicesEditSelect.enable();
        }
        suppliersChoicesEditSelect.enable();
        document.getElementById('saveEditAssetBtn').hidden = false;
        document.getElementById('editAssetBtn').hidden = true;
        document.getElementById('cancelBtn').hidden = false;
    } else {
        rootElement.querySelectorAll('.editField').forEach(elem => {
          elem.disabled = true;
          if (elem.tagName === "A") {
              elem.hidden = false;
              elem.nextElementSibling.hidden = true;
          }

          if (elem.classList.contains("datepicker")) {
            if (elem.value == null || elem.value === "") {
              elem.parentElement.hidden = true; // hide the datepicker
              elem.parentElement.nextElementSibling.hidden = false; // show the "ingen" textfield
            }
          }
        });
        suppliersChoicesEditSelect.disable();
        managersChoicesEditSelect.disable();
        document.getElementById('saveEditAssetBtn').hidden = true;
        document.getElementById('editAssetBtn').hidden = false;
        document.getElementById('cancelBtn').hidden = true;
    }
}
