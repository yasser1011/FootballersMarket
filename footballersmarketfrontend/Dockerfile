FROM node:18-alpine
RUN npm install -g serve
WORKDIR /footballersmarketappfrontend
COPY ./build /footballersmarketappfrontend/build

CMD ["serve", "-s", "build"]