
function truncateString(str, num) {
    if (str.length <= num) {
        return str
    }
    return str.slice(0, num) + '...'
}

const initSelect = (element) => {
    return new Choices(element, {
        searchChoices: false,
        removeItemButton: true,
        allowHTML: true,
        searchFloor: 0,
        searchPlaceholderValue: 'Søg...',
        itemSelectText: 'Vælg',
        noChoicesText: 'Søg...',
        classNames: {
            containerInner: 'form-control'
        },
        duplicateItemsAllowed: false,
    });
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
}
function checkInputField(my_choices) {
    let inner_element = my_choices.containerInner.element;
    if (my_choices.getValue(true)) {
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

async function postData(url = "", data = {}) {
    var token = document.getElementsByName("_csrf")[0].getAttribute("content");
    const response = await fetch(url, {
        method: "POST",
        headers: {
           'X-CSRF-TOKEN': token,
           "Content-Type": "application/json"
        },
        referrerPolicy: "no-referrer",
        body: JSON.stringify(data),
    });
    return response;
}

function asIntOrDefault(value, def) {
    const parsed = parseInt(value);
    if (isNaN(parsed)) {
        return def;
    }
    return parsed;
}

