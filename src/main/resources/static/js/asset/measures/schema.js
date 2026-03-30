import { refreshSchema, showSpinner, hideSpinner } from './services/schemaService.js';
import { showCategoryForm } from './services/categoryService.js';
import { showMeasureForm } from './services/measureService.js';

document.addEventListener("DOMContentLoaded", function() {
    initPage();
});

function initPage() {
    refreshSchema();
    initTopButtons();
}

function initTopButtons() {
    const createCategoryButton = document.getElementById("createCategoryButton");
    createCategoryButton?.addEventListener("click", () => showCategoryForm());

    const createMeasureButton = document.getElementById("createMeasureButton");
    createMeasureButton?.addEventListener("click", () => showMeasureForm());
}