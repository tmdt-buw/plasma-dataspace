# Statistical refinement recommender

An optional PLASMA Python auxiliary service that derives linked-model-extension recommendations from finalized semantic models. It maintains a recommendation index based on the configured semantic-model and ontology stores. It is separate from manual modeling and is not started by the lightweight Solid Dataspace PLASMA profile.

Use an isolated Python 3.11 environment with the service's pinned requirements. From this directory:

```sh
python3 -m venv .venv
. .venv/bin/activate
python3 -m pip install -r requirements-test.txt
```

The supported complete check is `./scripts/test-integration.sh` from the repository root. It starts disposable stores and runs the recommendation/API checks together with the core integration suite. Tests must not target existing production semantic-model stores. See the [root README](../../README.md) for prerequisites, environment variables and temporary ports.

For a separately configured service, inspect `service.py` and its configuration for registration URLs, store endpoints and index refresh behavior. Existing upstream license and authorship notices remain applicable.
