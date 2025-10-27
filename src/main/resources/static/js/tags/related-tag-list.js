import initTagSelect from "./tag-selector.js";

export default function initRelatedTagList(contextElementSelector) {
    const fragmentIdentifierClass = 'related-tag-list'
    let fragmentContainerElement
    if (contextElementSelector) {
        fragmentContainerElement = document.querySelector(contextElementSelector);
    } else {
        fragmentContainerElement = document.querySelector(`.${fragmentIdentifierClass}`).parentElement
    }
    initRemoveTagButtons(fragmentContainerElement)
    initAddTagButton()

    initTagSelect("addTagsSelect");
}

function initRemoveTagButtons(fragmentContainerElement) {

    const tagFragmentContainer = fragmentContainerElement.querySelector('.related-tag-list')
    const targetId = tagFragmentContainer.dataset.targetId;
    const targetType = tagFragmentContainer.dataset.targetType;

    if (fragmentContainerElement) {
        fragmentContainerElement.addEventListener('click', async (e) => {
            const target = e.target.closest(`.removeTagButton`);
            if (target) {
                e.stopPropagation();
                await removeTag(target, targetId, targetType);
            }
        })
    }
}

async function removeTag(element, targetId, targetType) {
    const tagId = element.dataset.tagid;

    if (!targetId || !targetType) {
        console.error('could not find target attributes')
        return;
    }

    const token = document.getElementsByName("_csrf")[0].getAttribute("content");

    const url = `/rest/tags/${tagId}/remove/${targetType}/${targetId}`

    const result = await fetch(url, {
        method: 'DELETE',
        headers: {
            'X-CSRF-TOKEN': token,
        }
    });

    if (result.ok) {
        location.reload();
    }
}

function initAddTagButton() {
    const addTagButton = document.getElementById('addTagButton');
    if (!addTagButton) {
        return;
    }

    addTagButton.addEventListener('click', async (e) => addTag(addTagButton))
}

async function addTag(buttonElement) {
    const targetId = buttonElement.dataset.targetId;
    const targetType = buttonElement.dataset.targetType;

    if (!targetId || !targetType) {
        return
    }
    const url = `/rest/tags/add/${targetType}/${targetId}`;
    const select = document.getElementById('addTagsSelect');
    if (!select) {
        return;
    }
    const selectedTagIds = [...select.selectedOptions].map((el) => el.value);

    const response = await fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': token,
        },
        body: JSON.stringify(selectedTagIds),
    })

    if (!response.ok) {
        toastService.error("Kunne ikke tilføje dette tag");
    }

    location.reload();

}