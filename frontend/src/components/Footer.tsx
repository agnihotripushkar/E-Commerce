import Link from 'next/link';

export default function Footer() {
  return (
    <footer className="bg-gray-900 text-gray-400 mt-20">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          <div>
            <div className="flex items-center gap-2 mb-3">
              <span className="text-2xl">🧘</span>
              <span className="text-white font-bold text-xl">ZenFlow</span>
            </div>
            <p className="text-sm leading-relaxed">
              Premium yoga and fitness gear for your mindful movement journey.
            </p>
          </div>
          <div>
            <h4 className="text-white font-semibold mb-3">Shop</h4>
            <ul className="space-y-2 text-sm">
              <li><Link href="/products" className="hover:text-white transition-colors">All Products</Link></li>
              <li><Link href="/products?category=Mats" className="hover:text-white transition-colors">Yoga Mats</Link></li>
              <li><Link href="/products?category=Equipment" className="hover:text-white transition-colors">Equipment</Link></li>
              <li><Link href="/products?category=Apparel" className="hover:text-white transition-colors">Apparel</Link></li>
            </ul>
          </div>
          <div>
            <h4 className="text-white font-semibold mb-3">Account</h4>
            <ul className="space-y-2 text-sm">
              <li><Link href="/auth/login" className="hover:text-white transition-colors">Sign In</Link></li>
              <li><Link href="/auth/register" className="hover:text-white transition-colors">Create Account</Link></li>
              <li><Link href="/orders" className="hover:text-white transition-colors">Order History</Link></li>
              <li><Link href="/cart" className="hover:text-white transition-colors">Cart</Link></li>
            </ul>
          </div>
        </div>
        <div className="border-t border-gray-800 mt-10 pt-6 text-sm text-center">
          © {new Date().getFullYear()} ZenFlow. Built with Spring Boot & Next.js.
        </div>
      </div>
    </footer>
  );
}
