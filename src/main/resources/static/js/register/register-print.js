const FALLBACK_FILE_NAME = 'artikel30.docx';

function getCsrfToken() {
    return document.getElementsByName('_csrf')[0]?.getAttribute('content');
}

function getFileName(response) {
    const header = response.headers.get('Content-Disposition') ?? '';

    const encoded = /filename\*=UTF-8''([^;]+)/i.exec(header);
    if (encoded) {
        return decodeURIComponent(encoded[1]);
    }

    const plain = /filename="([^"]+)"/i.exec(header);
    return plain ? plain[1] : FALLBACK_FILE_NAME;
}

function saveBlob(blob, fileName) {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
}

function setButtonLoading(button, loading) {
    if (!button) {
        return;
    }

    button.disabled = loading;
    button.querySelector('i')?.toggleAttribute('hidden', loading);
    button.querySelector('.spinner-border')?.toggleAttribute('hidden', !loading);
}

async function downloadReport(url, options, button) {
    setButtonLoading(button, true);

    try {
        const response = await fetch(url, options);

        // The server answers 400 when the filtered list is empty
        if (response.status === 400) {
            toastService.error('Der er ingen behandlingsaktiviteter at udskrive');
            return;
        }
        if (!response.ok) {
            throw new Error(`Unexpected status ${response.status}`);
        }

        saveBlob(await response.blob(), getFileName(response));
    } catch (error) {
        toastService.error('Udskrivning fejlede. Prøv igen.');
    } finally {
        setButtonLoading(button, false);
    }
}

export function printRegister(registerId, button = null) {
    return downloadReport(`/rest/registers/${registerId}/print`, {}, button);
}

export function printFilteredRegisters(customGridFunctions, button = null) {
    return downloadReport('/rest/registers/print', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': getCsrfToken()
        },
        body: JSON.stringify({ filters: customGridFunctions.getFilters() })
    }, button);
}