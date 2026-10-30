const DEPLOYER = 'DEPLOYER';
const PROHIBITED = 'PROHIBITED';
const REQUIREMENT_LEVELS_BY_RISK = {
    LIMITED: ['LIMITED'],
    HIGH: ['LIMITED', 'HIGH'],
};
const PERSONAL_DATA_FOLLOWUP_TRIGGERS = ['YES', 'NEEDS_CLARIFICATION'];

const tab = document.getElementById('aiTab');
const baseUrl = `/rest/assets/${tab?.dataset.assetId}/ai`;
const roleSelect = document.getElementById('aiActorRole');
const riskSelect = document.getElementById('aiRiskFactor');
const riskNote = document.getElementById('aiRiskNote');
const gateRow = document.querySelector('#aiRiskTable .ai-answer-row');

export function initAi() {
    if (!tab) {
        return;
    }

    const changeable = tab.dataset.changeable === 'true';
    initSelect(roleSelect, 'form-control', { shouldSort: false, readOnly: !changeable });
    initSelect(riskSelect, 'form-control', { shouldSort: false, readOnly: !changeable });
    initCollapseLabels();

    if (changeable) {
        initListeners();
        updateNoteStates();
    } else {
        tab.querySelectorAll('select, textarea').forEach(element => {
            element.disabled = true;
        });
    }

    refreshVisibility();
}

function initListeners() {
    roleSelect.addEventListener('change', onRoleChange);
    riskSelect.addEventListener('change', onRiskChange);
    riskNote.addEventListener('change', saveRisk);
    tab.querySelectorAll('.ai-answer-row').forEach(initAnswerRow);
}

function initAnswerRow(row) {
    row.querySelector('.ai-answer').addEventListener('change', () => onAnswerChange(row));
    row.querySelector('.ai-answer-note').addEventListener('change', () => saveAnswer(row));
}

async function onRoleChange() {
    refreshVisibility();
    await save('actor-role', { actorRole: roleSelect.value || null });
}

async function onRiskChange() {
    const riskFactor = riskSelect.value;
    if (!riskFactor || riskFactor === PROHIBITED) {
        clearAnswers();
    }
    updateNoteStates();
    expand('aiRequirementsCollapse');
    refreshVisibility();
    await saveRisk();
}

async function onAnswerChange(row) {
    updateRowNoteState(row);
    if (row === gateRow) {
        expand('aiPersonalDataCollapse');
        refreshVisibility();
    }
    await saveAnswer(row);
}

function refreshVisibility() {
    const role = roleSelect.value;
    const riskFactor = riskSelect.value;
    const gateVisible = !!riskFactor && riskFactor !== PROHIBITED;

    document.getElementById('aiActorRoleInfo').hidden = !role || role === DEPLOYER;
    document.getElementById('aiDeployerSection').hidden = role !== DEPLOYER;

    showRequirementsFor(riskFactor);

    gateRow.hidden = !gateVisible;
    document.getElementById('aiPersonalData').hidden = !gateVisible
        || !PERSONAL_DATA_FOLLOWUP_TRIGGERS.includes(gateRow.querySelector('.ai-answer').value);
}

function showRequirementsFor(riskFactor) {
    const levels = REQUIREMENT_LEVELS_BY_RISK[riskFactor] ?? [];
    const rows = [...document.querySelectorAll('#aiRequirements .ai-answer-row')];
    rows.forEach(row => {
        row.hidden = !levels.includes(row.dataset.riskLevel);
    });
    document.getElementById('aiRequirements').hidden = rows.every(row => row.hidden);
}

function updateNoteStates() {
    updateNoteState(riskSelect, riskNote);
    tab.querySelectorAll('.ai-answer-row').forEach(updateRowNoteState);
}

function updateRowNoteState(row) {
    updateNoteState(row.querySelector('.ai-answer'), row.querySelector('.ai-answer-note'));
}

function updateNoteState(select, note) {
    note.disabled = !select.value;
    if (note.disabled) {
        note.value = '';
    }
}

function clearAnswers() {
    tab.querySelectorAll('.ai-answer-row').forEach(clearAnswer);
}

function clearAnswer(row) {
    row.querySelector('.ai-answer').value = '';
    row.querySelector('.ai-answer-note').value = '';
}

function initCollapseLabels() {
    tab.querySelectorAll('[data-ai-collapse-label]').forEach(initCollapseLabel);
}

function initCollapseLabel(button) {
    const collapse = document.querySelector(button.dataset.bsTarget);
    const label = button.dataset.aiCollapseLabel;
    collapse.addEventListener('show.bs.collapse', () => {
        button.textContent = `Skjul ${label}`;
    });
    collapse.addEventListener('hide.bs.collapse', () => {
        button.textContent = `Vis ${label}`;
    });
}

function expand(collapseId) {
    const collapse = document.getElementById(collapseId);
    if (!collapse.classList.contains('show')) {
        bootstrap.Collapse.getOrCreateInstance(collapse).show();
    }
}

async function saveRisk() {
    await save('risk', { riskFactor: riskSelect.value || null, note: riskNote.value });
}

async function saveAnswer(row) {
    await save(`answers/${row.dataset.questionId}`, {
        answer: row.querySelector('.ai-answer').value || null,
        note: row.querySelector('.ai-answer-note').value,
    });
}

async function save(path, body) {
    const token = document.getElementsByName('_csrf')[0]?.getAttribute('content');
    const saved = await fetch(`${baseUrl}/${path}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
            ...(token ? { 'X-CSRF-TOKEN': token } : {}),
        },
        body: JSON.stringify(body),
    }).then(response => response.ok, () => false);

    if (saved) {
        toastService.info('Gemt');
    } else {
        toastService.error('Der skete en fejl under gemning');
    }
}
