
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

function sessionExpiredHandler() {
    toastService.error("Din session er udløbet, genindlæser");
    setTimeout(function () {
        location.reload();
    }, 3000);
}

function defaultResponseErrorHandler(response) {
    if (response.status === 403) {
        sessionExpiredHandler();
    }
    if (!response.ok) {
        throw new Error(`${response.status} ${response.statusText}`);
    }
}

const defaultResponseHandler = function (response) {
    defaultResponseErrorHandler(response);
    toastService.info("Info", "Dine ændringer er blevet gemt");
}

const defaultErrorHandler = function(error) {
    toastService.error(error);
    console.error(error);
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
        body: data != null ? JSON.stringify(data) : null,
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
            if (response.status === 403) {
                sessionExpiredHandler();
                return;
            }
            if (!response.ok) {
                throw new Error(`${response.status} ${response.statusText}`);
            }
            return response.text().then(data => {
                document.getElementById(targetId).innerHTML = data;
            });
        }
    ).catch(defaultErrorHandler);
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


/**
 * Implements a listener, running the callback a set time after the inputs last keyup event.
 * timeout is in miliseconds.
 */
class InputTimer {
    #inputElement
    #timeout
    #callback
    #timerId

    constructor(inputElement, timeout = 1000, callback){
        this.#inputElement = inputElement,
        this.#timeout = timeout  ,
        this.#callback = callback

        if(this.#inputElement && this.#callback) {
            this.#initInput()
        } else {
            console.error('Could not initialize InputTimer on element '+inputElement+' with callback '+callback)
        }
    }

    #initInput() {
        this.#inputElement.addEventListener('keyup', () => {
            clearTimeout(this.#timerId);
                this.#timerId = setTimeout(this.#callback, this.#timeout);
        });
    }


};

class NetworkService {
    XCSRFToken
    loadingMessage
    loadingClass
    #loadingElement

    /**
    * Creates a new NetworkService instance
    * @param {string} XCSRFToken the X-CSRF token to use for requests. If not defined, will attempt to use any accessable variable called 'token'
    * @param {string} loadingMessage The message shown while fragment loads
    * @param {string} loadingClass the class applied to the loading div while a fragment loads
    */
    constructor(XCSRFToken = token, loadingMessage = 'Henter...', loadingClass = 'loading'){
        this.XCSRFToken = XCSRFToken
        this.loadingMessage = loadingMessage
        this.loadingClass = loadingClass

        this.#loadingElement = document.createElement('div')
        this.#loadingElement.classList.add(this.loadingClass)
        this.#loadingElement.textContent = this.loadingMessage


    }

    /**
    * Fetches a fragment from the provided url and inserts it into the provided container, meanwhile showing a loading message
    * @param {string} url
    * @param {HTMLElement} containerElement
    * @returns the response ok status
    */
    async GetFragment(url, containerElement) {
        if (!this.XCSRFToken || !token) {
            throw new Error(`X-CSRF token not defined. NetworkService methods requires the token variable to be defined in the document`)
        }
        if (!containerElement) {
            throw new Error('No container element defined. A container must be provided to contain the fragment')
        }

        containerElement.appendChild(this.#loadingElement)

        const response = await fetch (url, {
            headers: {
                'X-CSRF-TOKEN': this.XCSRFToken || token,
            }
        })

        if (!response.ok) {
            throw new Error(`Error when getting url: ${url}`)
        }

        containerElement.innerHTML = await response.text()

        return response.ok
    }

    /**
    * Fetch GET's from a url, parsing the result from json to JS object
    * @param {string} url
    * @returns the response as JS object
    */
    async Get(url) {
        if (!this.XCSRFToken && !token) {
            throw new Error(`X-CSRF token not defined. NetworkService methods requires the token variable to be defined in the document`)
        }

        const response = await fetch (url, {
            headers: {
                'X-CSRF-TOKEN': this.XCSRFToken || token,
            }
        })

        if (!response.ok) {
            throw new Error(`Error when getting url: ${url}`)
        }

        return await response.json()
    }

    /**
    * Fetch POST's data to the provided url as json, returning the parsed response as JS object
    * @param {string} url
    * @param {Object} data
    * @returns response as JS object
    */
    async Post (url, data) {
        if (!this.XCSRFToken && !token) {
            throw new Error(`X-CSRF token not defined. NetworkService methods requires the token variable to be defined in the document`)
        }

        const response = await fetch (url, {
            method: 'POST',
            headers: {
                'X-CSRF-TOKEN': this.XCSRFToken || token,
                "Content-Type": "application/json",
            },
            body: JSON.stringify(data)
        })

        if (!response.ok) {
            throw new Error(`Error when posting to url: ${url}`)
        }

        return await response.json()
    }

    /**
    * Fetch PUT's the data to the provided url as json, returning the parsed response as JS object
    * @param {string} url
    * @param {Object} data
    * @returns response as JS object
    */
    async Put (url, data){

        if (!this.XCSRFToken && !token) {
            throw new Error(`X-CSRF token not defined. NetworkService methods requires the token variable to be defined in the document`)
        }

        const response = await fetch (url, {
            method: 'PUT',
            headers: {
                'X-CSRF-TOKEN': this.XCSRFToken || token,
                "Content-Type": "application/json",
            },
            body: JSON.stringify(data)
        })

        if (!response.ok) {
            throw new Error(`Error when posting to url: ${url}`)
        }

        return await response.json()
    }

    /**
    * Fetch DELETE's to the provided url, returning the parsed response as JS object
    * @returns response as JS object
    */
    async Delete (url) {
        if (!this.XCSRFToken && !token) {
            throw new Error(`X-CSRF token not defined. NetworkService methods requires the token variable to be defined in the document`)
        }

        const response = await fetch (url, {
            method: 'DELETE',
            headers: {
                'X-CSRF-TOKEN': this.XCSRFToken || token,
            }
        })

        if (!response.ok) {
            throw new Error(`Error when getting url: ${url}`)
        }

        return await response.json()
    }

}
