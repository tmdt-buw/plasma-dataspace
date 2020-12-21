FROM nginx
# copy over the artifacts in dist folder to hidden nginx public folder
COPY /dist/plasma /usr/share/nginx/html

# copy a nginx config that works with angular
COPY nginx.conf /etc/nginx/conf.d/default.conf
