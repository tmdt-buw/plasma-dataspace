FROM nginx
# copy over the artifacts in dist folder to hidden nginx public folder
COPY /dist/plasma-minimal-ui /usr/share/nginx/html
# copy a nginx config that works with angular
COPY nginx.conf.template /etc/nginx/conf.d/default.conf
