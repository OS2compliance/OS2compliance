export default class NetworkService {
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

        const text = await response.text();
        if (!text) {
            return {};
        }

        try {
            return JSON.parse(text);
        } catch (error) {
            console.error('Invalid JSON response:', text);
            throw new Error(`Invalid JSON response from ${url}: ${text}`);
        }
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