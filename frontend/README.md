# PLASMA Frontend

This project was generated with [Angular CLI](https://github.com/angular/angular-cli).

## Common Library

1. common - Common components

## Services

In the following services `{service}` are:

1. dss - Data source service
2. dms - Data modeling service
3. sas - Schema analysis service 

## Client generation from backend

Run `npm run gen:{service}` to generate updated http-client and model from backend openapi-specs.

## Development server

Run `npm run watch:common` to build library. The library will automatically reload if you change any of the source files.

Run `npm run watch:{service}` to build library. The library will automatically reload if you change any of the source files.

Run `npm start` for a dev server. Navigate to `http://localhost:4200/`. The app will automatically reload if you change any of the source files.
