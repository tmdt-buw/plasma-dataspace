FROM node:lts-alpine AS build

ARG PLASMA_VERSION
ARG PLASMA_BASE_HREF=/
ARG PLASMA_API_BASE_URL=

WORKDIR /home/
COPY ./package-lock.json ./
COPY ./package.json ./

RUN npm ci

COPY ./ ./

# Configure the immutable frontend bundle for its public mount point.
RUN sed -i \
    -e "s|__APP_VERSION__|${PLASMA_VERSION}|g" \
    -e "s|__PLASMA_API_BASE_URL__|${PLASMA_API_BASE_URL}|g" \
    ./src/environments/environment.prod.ts

RUN npm run build:smui -- --configuration=production
RUN npm run build -- \
    --configuration=production \
    --base-href="${PLASMA_BASE_HREF}"

FROM nginx
# copy over the artifacts in dist folder to hidden nginx public folder
COPY --from=build /home/dist/plasma /usr/share/nginx/html

# copy a nginx config that works with angular
COPY nginx.conf /etc/nginx/conf.d/default.conf
