"use client";
import React from "react";
import {
  motion,
  useScroll,
  useTransform,
  useSpring,
  MotionValue,
} from "framer-motion";
import Image from "next/image";
import Link from "next/link";
import { ArrowRight } from "lucide-react";

export const HeroParallax = ({
  products,
}: {
  products: {
    title: string;
    link: string;
    thumbnail: string;
  }[];
}) => {
  const firstRow = products.slice(0, 5);
  const secondRow = products.slice(5, 10);
  const thirdRow = products.slice(10, 15);
  const ref = React.useRef(null);
  const { scrollYProgress } = useScroll({
    target: ref,
    offset: ["start start", "end start"],
  });

  const springConfig = { stiffness: 300, damping: 30, bounce: 100 };

  const translateX = useSpring(
    useTransform(scrollYProgress, [0, 1], [0, 1000]),
    springConfig
  );
  const translateXReverse = useSpring(
    useTransform(scrollYProgress, [0, 1], [0, -1000]),
    springConfig
  );
  const rotateX = useSpring(
    useTransform(scrollYProgress, [0, 0.2], [15, 0]),
    springConfig
  );
  const opacity = useSpring(
    useTransform(scrollYProgress, [0, 0.2], [0.2, 1]),
    springConfig
  );
  const rotateZ = useSpring(
    useTransform(scrollYProgress, [0, 0.2], [20, 0]),
    springConfig
  );
  const translateY = useSpring(
    useTransform(scrollYProgress, [0, 0.2], [-700, 500]),
    springConfig
  );
  
  return (
    <div
      ref={ref}
      className="h-[250vh] md:h-[300vh] py-10 overflow-hidden antialiased relative flex flex-col self-auto [perspective:1000px] [transform-style:preserve-3d] pt-16 md:pt-24"
    >
      <Header />
      <motion.div
        style={{
          rotateX,
          rotateZ,
          translateY,
          opacity,
        }}
        className=""
      >
        <motion.div className="flex flex-row-reverse space-x-reverse space-x-10 md:space-x-20 mb-10 md:mb-20">
          {firstRow.map((product) => (
            <ProductCard
              product={product}
              translate={translateX}
              key={product.title}
            />
          ))}
        </motion.div>
        <motion.div className="flex flex-row mb-10 md:mb-20 space-x-10 md:space-x-20 ">
          {secondRow.map((product) => (
            <ProductCard
              product={product}
              translate={translateXReverse}
              key={product.title}
            />
          ))}
        </motion.div>
        <motion.div className="flex flex-row-reverse space-x-reverse space-x-10 md:space-x-20">
          {thirdRow.map((product) => (
            <ProductCard
              product={product}
              translate={translateX}
              key={product.title}
            />
          ))}
        </motion.div>
      </motion.div>
      
      {/* Fade out bottom to blend with next section */}
      <div className="absolute bottom-0 w-full h-[500px] bg-gradient-to-t from-primary to-transparent z-10 pointer-events-none" />
    </div>
  );
};

export const Header = () => {
  return (
    <div className="max-w-7xl relative mx-auto py-10 md:py-20 px-6 w-full left-0 top-0 z-20 pointer-events-auto mt-[-2rem] md:mt-0">
      <div className="mb-6 inline-flex items-center gap-2 bg-white/50 backdrop-blur-md border border-white/80 shadow-soft px-4 py-2 rounded-full">
        <span className="h-2 w-2 rounded-full bg-accent animate-pulse-soft" />
        <span className="text-[11px] font-bold tracking-widest uppercase text-accent">Adaptive Scheduling 2.0</span>
      </div>

      <h1 className="text-5xl md:text-8xl font-extrabold leading-[1.05] tracking-tight text-text-primary mb-6">
        Your time, <br className="hidden md:block"/>
        <span className="bg-clip-text text-transparent bg-gradient-to-r from-accent to-accent-light">mathematically perfected.</span>
      </h1>
      <p className="max-w-2xl text-lg md:text-xl text-text-secondary font-medium leading-relaxed mb-10">
        Stop waiting in line. Our unified platform calculates precise arrivals for Healthcare, Government, and Services. Stop dealing with 5 different unoptimized calendars.
      </p>
      
      <div className="flex flex-col sm:flex-row gap-4">
        <Link href="/explore/healthcare" className="btn-pastel-primary shadow-float">
          <span>Start Booking</span>
          <ArrowRight size={18} strokeWidth={2.5} />
        </Link>
      </div>
    </div>
  );
};

export const ProductCard = ({
  product,
  translate,
}: {
  product: {
    title: string;
    link: string;
    thumbnail: string;
  };
  translate: MotionValue<number>;
}) => {
  return (
    <motion.div
      style={{
        x: translate,
      }}
      whileHover={{
        y: -15,
      }}
      key={product.title}
      className="group/product h-64 w-[20rem] md:h-96 md:w-[30rem] relative flex-shrink-0 bg-white/30 backdrop-blur-sm p-2 rounded-3xl border border-white/50 shadow-soft"
    >
      <Link
        href={product.link}
        className="block group-hover/product:shadow-2xl h-full w-full relative overflow-hidden rounded-2xl"
      >
        <Image
          src={product.thumbnail}
          height="600"
          width="600"
          className="object-cover object-center absolute h-full w-full inset-0 transition-transform duration-500 group-hover/product:scale-105"
          alt={product.title}
        />
        <div className="absolute inset-0 h-full w-full opacity-0 group-hover/product:opacity-40 bg-gradient-to-t from-accent to-transparent transition-opacity duration-300 pointer-events-none"></div>
        <h2 className="absolute bottom-4 left-6 opacity-0 group-hover/product:opacity-100 text-white font-bold text-xl tracking-wide transition-opacity duration-300 translate-y-4 group-hover/product:translate-y-0">
          {product.title}
        </h2>
      </Link>
    </motion.div>
  );
};
