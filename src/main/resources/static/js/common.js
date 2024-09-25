
function truncateString(str, num) {
    if (str.length <= num) {
        return str
    }
    return str.slice(0, num) + '...'
}

const initSelect = (element, containerInner = 'form-control') => {
    let choices = new Choices(element, {
        searchChoices: false,
        removeItemButton: true,
        allowHTML: true,
        searchFloor: 0,
        searchPlaceholderValue: 'Søg...',
        itemSelectText: 'Vælg',
        noChoicesText: 'Søg...',
        classNames: {
            containerInner: containerInner
        },
        duplicateItemsAllowed: false,
    });
    element.addEventListener("change",
        function(event) {
            choices.hideDropdown();
        },
        false,
    );
    return choices;
}

function initDatepicker(elementQuerySelector, inputField) {
    const datePicker = MCDatepicker.create({
        el: inputField,
        autoClose: true,
        dateFormat: 'dd/mm-yyyy',
        //minDate: new Date(),
        closeOnBlur: true,
        firstWeekday: 1,
        customWeekDays: ["sø", "ma", "ti", "on", "to", "fr", "lø"],
        customMonths: ["Januar", "Februar", "Marts", "April", "Maj", "Juni", "Juli", "August", "September", "Oktober", "November", "December"],
        customClearBTN: "Ryd",
        customCancelBTN: "Annuller"
    });
    document.querySelector(elementQuerySelector).addEventListener( "click", () => {
        datePicker.open();
    });
    return datePicker;
}
function checkInputField(my_choices, atleastOne = false) {
    const inner_element = my_choices.containerInner.element;
    const value = my_choices.getValue(true);
    if (value && (!atleastOne || value.length > 0)) {
        inner_element.classList.remove('is-invalid');
        //check if there is a div right after "choices" element
        if (inner_element.parentElement.nextElementSibling) {
            inner_element.parentElement.nextElementSibling.classList.remove('show');
        }
        return true;
    } else {
        inner_element.classList.add('is-invalid');
        //check if there is a div right after "choices" element
        if (inner_element.parentElement.nextElementSibling) {
            inner_element.parentElement.nextElementSibling.classList.add('show');
        }
        return false;
    }
}

const defaultResponseHandler = function (response) {
    if (!response.ok) {
        throw new Error(`${response.status} ${response.statusText}`);
    }
    toastService.info("Info", "Dine ændringer er blevet gemt");
}

const defaultErrorHandler = function(error) {
    toastService.error(error);
    console.log(error);
}

async function jsonCall(method, url, data = {}) {
    var token = document.getElementsByName("_csrf")[0].getAttribute("content");
    return await fetch(url, {
        method: method,
        headers: {
            'X-CSRF-TOKEN': token,
            "Content-Type": "application/json"
        },
        referrerPolicy: "no-referrer",
        body: JSON.stringify(data),
    });
}

async function postData(url, data = {}) {
    return jsonCall("POST", url, data);
}

async function putData(url = "", data = {}) {
    return jsonCall("PUT", url, data);
}

async function deleteData(url) {
    return jsonCall("DELETE", url, {});
}


function asIntOrDefault(value, def) {
    const parsed = parseInt(value);
    if (isNaN(parsed)) {
        return def;
    }
    return parsed;
}

// Fetches the given url and puts the resulting html and innerHtml on the element with id of targetId
function fetchHtml(url, targetId) {
    return fetch(url)
        .then(response => {
            if (!response.ok) {
                throw new Error(`${response.status} ${response.statusText}`);
            }
            response.text().then(data => {
                document.getElementById(targetId).innerHTML = data;
            });
        }
    ).catch(error => { toastService.error(error); console.log(error) });
}

function javaFormatDate(date) {
    if (date === null || date === '') {
        return '';
    }
    let dd = "" + date.getDate();
    if (dd.length === 1) {
        dd = "0" + dd;
    }
    let mm = "" + (date.getMonth()+1);
    if (mm.length === 1) {
        mm = "0" + mm;
    }
    let yyyy = date.getFullYear();
    return `${yyyy}-${mm}-${dd}`;
}

function foregroundColorForHex(rrggbb) {
    if (rrggbb == null) {
        return "";
    }
    const red = parseInt(rrggbb.substring(1,3), 16);
    const green = parseInt(rrggbb.substring(3,5), 16);
    const blue = parseInt(rrggbb.substring(5,7), 16);
    const avg = (red + green + blue) / 3;
    return avg > 150 ? "black" : "white";
}

function ensureElementHasClass(elem, className) {
    if (!elem.classList.contains(className)) {
        elem.classList.add(className)
    }
}

