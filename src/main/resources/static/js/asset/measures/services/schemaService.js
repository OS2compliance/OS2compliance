import { bindCategoryEventListeners } from './categoryService.js';
import { bindMeasureEventListeners } from './measureService.js';

const STORAGE_KEY = 'openedMeasuresCategory';

export function showSpinner() {
    document.getElementById('spinner').style.display = 'block';
    document.getElementById('schemaPlaceholder').style.display = 'none';
}

export function hideSpinner() {
    document.getElementById('spinner').style.display = 'none';
    document.getElementById('schemaPlaceholder').style.display = 'block';
}

export async function refreshSchema() {
    showSpinner();

    try {
        const response = await fetch('/assets/measures/schema/fragment');
        if (!response.ok) {
            throw new Error('Kunne ikke hente skemaet');
        }

        const html = await response.text();
        const placeholder = document.getElementById('schemaPlaceholder');
        placeholder.innerHTML = html;

        initializeCategoryRows();
        bindCategoryEventListeners();
        bindMeasureEventListeners();

        hideSpinner();
    } catch (error) {
        toastService.error(error.message);
        hideSpinner();
    }
}

function initializeCategoryRows() {
    const categoryRows = document.querySelectorAll('.measuresSchemaCategoryTr');
    categoryRows.forEach(row => {
        const categoryId = row.dataset.categoryid;
        handleCategoryRow(categoryId);
        row.addEventListener('click', categoryRowClicked);
    });

    const openedCategory = sessionStorage.getItem(STORAGE_KEY);
    if (openedCategory) {
        handleCategoryRow(openedCategory);
    }
}

function categoryRowClicked() {
    const categoryId = this.dataset.categoryid;
    sessionStorage.setItem(STORAGE_KEY, categoryId);
    handleCategoryRow(categoryId);
}

function handleCategoryRow(categoryId) {
    const icon = document.getElementById("categoryIcon" + categoryId);
    const belongingRows = document.querySelectorAll('.categoryRow' + categoryId);

    if (!icon) {
        sessionStorage.removeItem(STORAGE_KEY);
        return;
    }

    let show = false;
    belongingRows.forEach((elem, index) => {
        if (elem.hidden) {
            if (index === 0) show = true;
            elem.hidden = false;
        } else {
            elem.hidden = true;
        }
    });

    icon.classList.toggle('pli-arrow-up', show);
    icon.classList.toggle('pli-arrow-down', !show);
}

export function bindEventListeners(selector, handler) {
    const elements = document.querySelectorAll(selector);
    elements.forEach(element => {
        element.addEventListener('click', handler);
    });
}