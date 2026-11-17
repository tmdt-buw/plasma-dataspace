# PLASMA semantic-modeling UI (SMUI)

PLASMA (Platform for Auxiliary Semantic Modeling Approaches) modeling is an angular library that offers a visual user interface for semantic modeling and ontology management in the plasma system.
PLASMA is a platform to utilize existing and future modeling approaches in a consistent and extendable environment. PLASMA aims to support the development of new semantic refinement approaches by providing necessary supplementary functionalities.

For this integration see the [fork README](../../../README.md). The [public upstream snapshot](https://github.com/tmdt-buw/plasma) documents the original project; it is not the current integration build.

## Cite PLASMA as

```
@conference{paulus21,
author={Alexander Paulus. and Andreas Burgdorf. and Lars Puleikis. and Tristan Langer. and André Pomp. and Tobias Meisen.},
title={PLASMA: Platform for Auxiliary Semantic Modeling Approaches},
booktitle={Proceedings of the 23rd International Conference on Enterprise Information Systems - Volume 2: ICEIS,},
year={2021},
pages={403-412},
publisher={SciTePress},
organization={INSTICC},
doi={10.5220/0010499604030412},
isbn={978-989-758-509-8},
issn={2184-4992},
}
```

## Use the modeling library

This checkout builds the local package `@tmdt/smui`; its package metadata is in `package.json`. Build it from the frontend workspace using `npm run build:smui`. The Angular 13 app and its lockfile provide the supported dependency combination. The historic `@tmdt-buw/pls-modeling` package name and broad peer ranges are not a guarantee of compatibility with arbitrary Angular releases.

Include the modeling component in your application providing at least a modeling id from a model created in plasma:

```
<pls-modeling [modelId]="modelId"></pls-modeling>
```

Options to customize the component:

| Property           | Description                                                                                                                         | Type    | Default |
|--------------------|-------------------------------------------------------------------------------------------------------------------------------------|---------|---------|
| modelId            | Id from a model create through the plasma api.                                                                                      | string  | -       |
| fullScreen         | Whether to show the button to switch to full screen modeling.                                                                       | boolean | true    |
| viewerMode         | If viewer modes is enabled, models can only be viewed but not edited. Viewer model will disable all model adjustment functionality. | boolean | false   |
| backgroundColor    | The background-color to use in the modeling canvas in hex code (mostly relevant for theming like dark mode).                        | string  | -       |
| hideFinalizeButton | Option to hide the finalize button to disable final model integration.                                                              | boolean | false   |
| hideExportButton   | Option to disable export via the GUI by hiding the export button.                                                                   | boolean | false   |

## Theming

The modeling library uses less variables for styling. The variables can be overwritten by creating a custom `modeling-theme.less` used by the project and overwrite the following variables:

```
@modeling-background-color: white;
@modeling-primary-color: #042042;
@modeling-text-color: white;
@modeling-highlight-color: #FC6B28;

// class colors
@modeling-class-color: @modeling-primary-color;
@modeling-class-text-color: white;

// named entity colors
@modeling-named-entity-color: #019267;
@modeling-named-entity-text-color: white;

// literal colors
@modeling-literal-color: #7c3e66;
@modeling-literal-text-color: white;

// relation colors
@modeling-objectproperty-color: @modeling-primary-color;
@modeling-datatypeproperty-color: @modeling-primary-color;
```

---

## Development

Run these commands from `frontend/`:

```sh
npm ci
npm run build:smui
npm test
```

The build output is `dist/@tmdt/smui`, as configured in `ng-package.json`. `npm test` runs the application and library checks. For live changes use `npm run watch:smui` alongside `npm start`. This fork does not require publishing the Angular library to npm to run PLASMA. Retain the upstream citation and the license declared in this library's package when distributing it.
