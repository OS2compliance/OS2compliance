let editTemplate, deleteTemplate, copyTemplate;

/**
 * Used to format the "Actions" column of a grid. Assumes there are editbutton and deletebutton templates in the HTML
 * Will assume the "cell" parameter holds an array of AllowedActions, which determines update and delete permissions for the user
 * Will set the id as an 'identifier' data attribute on the buttons. Will also set the name as a data attribute on the delete button
 * @param cell cell parameter passed from grid formatter
 * @param row row parameter passed from grid formatter
 * @param additionalDataAttributeMap a map describing data attributes that is to be set on the edit buttons. Must conform to data attribute requirements
 * @returns {*}
 */
function formatAllowedActions(cell, row, additionalDataAttributeMap = new Map()) {
    const container = document.createElement('span');

    if (cell?.includes("UPDATE")) {
        const buttonFragment = getEditTemplate()?.content.cloneNode(true);
        const button = buttonFragment.firstElementChild;

        for (const [key, value] of additionalDataAttributeMap.entries()) {
            button.dataset[key] = value;
        }
        container.appendChild(button);
    }
    if (cell?.includes("COPY")) {
        const buttonFragment = getCopyTemplate()?.content.cloneNode(true);
        const button = buttonFragment.firstElementChild;

        for (const [key, value] of  additionalDataAttributeMap.entries()) {
            button.dataset[key] = value;
        }
        container.appendChild(button);
    }
    if (cell?.includes("DELETE")) {
        const buttonFragment = getDeleteTemplate()?.content.cloneNode(true);
        const button = buttonFragment.firstElementChild;

        for (const [key, value] of  additionalDataAttributeMap.entries()) {
            button.dataset[key] = value;
        }
        container.appendChild(button);
    }

    return container.innerHTML; // Ugly hack because grid.js sucks
}

function getEditTemplate() {
    if (!editTemplate) {
        editTemplate = document.getElementById('editListItemButtonTemplate')
    }
    return editTemplate;
}

function getDeleteTemplate() {
    if (!deleteTemplate) {
        deleteTemplate = document.getElementById('deleteListItemButtonTemplate')
    }
    return deleteTemplate;
}

function getCopyTemplate() {
    if (!copyTemplate) {
        copyTemplate = document.getElementById('copyListItemButtonTemplate')
    }
    return copyTemplate;
}

/**
 * Initializes event delegation for the table with the given ID. Assigns the provided functions to the edit and delete button on click.
 * @param tableId Id of the parent table element
 * @param editAction Function to run when edit button is clicked. Is passed the ID of the row as parameter, and the button element itself for data-attribute extraction purposes
 * @param deleteAction Function to run when the delete button is clicked. Is passed ID and name of the row,and the button element itself for data-attribute extraction purposes
 * @param copyAction Function to run when copy button is clicked, Is passed the ID of the row as parameter, and the button element itself for data-attribute extraction purposes
 */
function delegateListItemActions(tableId, editAction = (id, element) => {}, deleteAction = (id, name, element) => {}, copyAction = (id, element) => {}) {
    const table = document.getElementById(tableId);

    table.addEventListener("click", (e) => {
        const target = e.target;
        if (target?.classList.contains('editBtn')) {
            const id = target.dataset.identifier;
            editAction(id, target)
        } else if (target?.classList.contains('deleteBtn')) {
            const id = target.dataset.identifier;
            const name = target.dataset.name;
            deleteAction(id, name, target)
        }else if (target?.classList.contains('copyBtn')) {
            const id = target.dataset.identifier;
            copyAction(id, target)
        }
    })
}