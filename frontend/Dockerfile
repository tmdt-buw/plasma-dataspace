FROM node:lts-alpine AS build

ARG PLASMA_VERSION

WORKDIR /home/
COPY ./package-lock.json ./
COPY ./package.json ./

RUN npm install

COPY ./ ./

# replace version string in frontend
RUN sed -i "s/__APP_VERSION__/${PLASMA_VERSION}/g" ./src/environments/environment.prod.ts

RUN npm run build:smui -- --configuration=production
RUN npm run build -- --configuration=production

FROM nginx
# copy over the artifacts in dist folder to hidden nginx public folder
COPY --from=build /home/dist/plasma /usr/share/nginx/html

# copy a nginx config that works with angular
COPY nginx.conf /etc/nginx/conf.d/default.conf
