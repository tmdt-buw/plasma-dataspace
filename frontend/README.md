# PLASMA frontend

Angular 13 application and the semantic-modeling UI library in `projects/smui`. It needs the SMS/DPS backend services; the library alone is not a complete modeling server. See the [integration README](../README.md) for Docker startup, upstream status and Dataspace routing.

## Install and build

Use the npm lockfile and the Node/toolchain selected by the Docker/CI build for this older Angular workspace. From this directory:

```sh
npm ci
npm run build:smui
npm run build
```

The local library package is `@tmdt/smui`, built to `dist/@tmdt/smui`. Do not use the old `projects/modeling` or `ng build modeling` paths. The application is built to `dist/plasma`.

`Dockerfile` accepts `PLASMA_VERSION`, `PLASMA_BASE_HREF` and `PLASMA_API_BASE_URL`. The published Dataspace image uses `/plasma/` as base href and `/plasma` for API requests. The independent root Compose setup uses its own root-mounted proxy configuration. These are build-time settings.

## Development

In one terminal:

```sh
npm run watch:smui
```

In another:

```sh
npm start
```

The app runs at `http://localhost:4200/`; configure the backend/proxy for the selected environment. The development server's host options are for local development, not public deployment.

## Tests and generated clients

```sh
npm test
```

This builds SMUI and runs both Angular test suites in headless Chrome. Set `CHROME_BIN` if required.

With SMS/DPS reachable through the configured OpenAPI URLs:

```sh
npm run gen:sms
npm run gen:dps
```

The generated API clients and their READMEs are generator output. Regenerate them together when an API contract changes. The generation commands in `package.json` are the source of truth; the old template `lint`/`e2e` scripts are not the verification workflow described here.
